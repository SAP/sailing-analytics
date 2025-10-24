package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.gwt.ui.adminconsole.StructureImportListComposite.RegattaStructureProvider;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.adminconsole.swisstiming.SwissTimingEventIdUrlUtil;
import com.sap.sailing.gwt.ui.client.Displayer;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventAndRegattaDTO;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.busyindicator.BusyIndicator;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class StructureImportManagementPanel extends SimplePanel implements RegattaStructureProvider {
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final Presenter presenter;
    private final StructureImportListComposite regattaListComposite;
    private final BusyIndicator busyIndicator;
    private TextBox jsonURLTextBox;
    private TextBox eventIDTextBox;
    private CaptionPanel regattaImportPanel;
    private Button listRegattasButton;
    private Button importDetailsButton;
    private VerticalPanel editSeriesPanel;
    private final FlexTable regattaStructureGrid;
    private List<EventDTO> existingEvents;
    private ListBox sailingEventsListBox;
    
    private final Displayer<EventDTO> eventsDisplayer;
    
    /**
     * Holds one {@link RegattaDTO} for each distinct regatta structure. The user can work with this panel
     * to adjust the defaults for each structure recognized, working with the table selection to show/hide
     * structures related to regattas in the list. After having set the defaults, pressing the "Import Regatta..."
     * button will apply these defaults during the creation of the regattas selected. When modifying the
     * defaults such that a different {@link RegattaStructure} will be needed to describe the modified version,
     * the old entry shall be removed and a new one with the {@link RegattaStructure} matching the modified
     * {@link RegattaDTO} shall be inserted.
     */
    private final Map<RegattaStructure, RegattaDTO> regattaDefaultsPerStructure;
    
    /**
     * Maps from the {@link RegattaDTO}s coming from the XRR import to the structure to be used when creating
     * the regatta in the back-end. When a regatta is first added to this map, the {@link RegattaStructure} object
     * is originally created using the key as construction parameter. Over time, the user may evolve the structure
     * to be used for import using the editing UIs, leading to a different {@link RegattaStructure} to be associated
     * where the key is no longer part of the equivalence class defined by the {@link RegattaStructure} value.
     * A typical case is a modification in the fleet structure, e.g., because the fleets weren't correctly recognized
     * during the import and required some manual intervention.<p>
     * 
     * The default settings to be used when finally creating the regatta in the back-end are maintained
     * in {@link #regattaDefaultsPerStructure} for which the values of this map can be used as key.
     */
    private final Map<RegattaDTO, RegattaStructure> regattaStructures;
    
    public StructureImportManagementPanel(final Presenter presenter, final StringMessages stringMessages) {
        this.regattaDefaultsPerStructure = new HashMap<>();
        this.regattaStructures = new HashMap<>();
        this.busyIndicator = new SimpleBusyIndicator();
        this.sailingServiceWrite = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.stringMessages = stringMessages;
        this.presenter = presenter;
        this.regattaListComposite = new StructureImportListComposite(presenter, this, errorReporter, stringMessages);
        this.eventsDisplayer = result->fillEvents(result);
        regattaListComposite.ensureDebugId("RegattaListComposite");
        VerticalPanel mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        createUI(mainPanel);
        regattaListComposite.addSelectionChangeHandler(new Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                updateRegattaStructureGrid();
                updateImportRegattasButtonEnabledness();
            }
        });
        regattaStructureGrid = new FlexTable();
    }

    private void createUI(VerticalPanel mainPanel) {        
        final Panel progressPanel = new FlowPanel();
        progressPanel.add(busyIndicator);
        eventIDTextBox = new TextBox();
        eventIDTextBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                final String url = SwissTimingEventIdUrlUtil.getUrlFromEventId(eventIDTextBox.getValue());
                jsonURLTextBox.setText(url);
            }
        });
        eventIDTextBox.ensureDebugId("eventIDTextBox");
        eventIDTextBox.setVisibleLength(50);
        jsonURLTextBox = new TextBox();
        jsonURLTextBox.ensureDebugId("JsonURLTextBox");
        jsonURLTextBox.setVisibleLength(100);
        jsonURLTextBox.getElement().setPropertyString("placeholder", SwissTimingEventIdUrlUtil.getUrlFromEventId("d30883d3-2876-4d7e-af49-891af6cbae1b"));
        listRegattasButton = new Button(this.stringMessages.listRegattas());
        importDetailsButton = new Button(this.stringMessages.importRegattas());
        importDetailsButton.setEnabled(false);
        importDetailsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (getSelectedEvent() != null) {
                    List<RegattaDTO> selectedOriginalRegattasFromXRR = regattaListComposite.getSelectedRegattas();
                    if (!selectedOriginalRegattasFromXRR.isEmpty()) {
                        createRegattas(selectedOriginalRegattasFromXRR, getSelectedEvent());
                    } else {
                        errorReporter.reportError(stringMessages.pleaseSelectAtLeastOneRegatta());
                    }
                } else {
                    errorReporter.reportError(stringMessages.pleaseSelectAnEvent());
                }
            }
        });
        listRegattasButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                listRegattasAndTheirStructures();
            }
        });
        Grid eventURLGrid = new Grid(3, 2);
        eventURLGrid.setWidget(0, 0, new Label(stringMessages.manage2SailEvent() + stringMessages.id() + ":"));
        eventURLGrid.setWidget(0, 1, eventIDTextBox);
        eventURLGrid.setWidget(1, 0, new Label(stringMessages.jsonUrl() + ":"));
        eventURLGrid.setWidget(1, 1, jsonURLTextBox);
        eventURLGrid.setWidget(2, 1, listRegattasButton);
        mainPanel.add(progressPanel);
        mainPanel.add(eventURLGrid);
        regattaImportPanel = new CaptionPanel(stringMessages.manage2SailEvent());
        mainPanel.add(regattaImportPanel);
        VerticalPanel regattaImportContentPanel = new VerticalPanel();
        regattaImportPanel.setContentWidget(regattaImportContentPanel);
        editSeriesPanel = new VerticalPanel();
        HorizontalPanel hPanel = new HorizontalPanel();
        hPanel.setSpacing(10);
        hPanel.add(regattaListComposite);
        hPanel.setCellWidth(regattaListComposite, "50%");
        hPanel.add(editSeriesPanel);
        hPanel.setCellWidth(editSeriesPanel, "50%");
        regattaImportContentPanel.add(hPanel);
        regattaImportContentPanel.add(importDetailsButton);
    }

    /**
     * Adds an event selector and a "create new event" button to the {@link #editSeriesPanel} in its first row
     */
    private Widget createEventSelectionAndCreateEventButtonUI() {
        final Grid grid = new Grid(1, 2);
        sailingEventsListBox = new ListBox();
        sailingEventsListBox.setMultipleSelect(false);
        sailingEventsListBox.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                updateImportRegattasButtonEnabledness();
            }
        });
        presenter.getEventsRefresher().callFillAndReloadInitially(getEventsDisplayer());
        sailingEventsListBox.ensureDebugId("EventListBox");
        grid.setWidget(0, 0, sailingEventsListBox);
        final Button newEventBtn = new Button(stringMessages.createNewEvent());
        newEventBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                openEventCreateDialog();
            }
        });
        grid.setWidget(0, 1, newEventBtn);
        editSeriesPanel.add(grid);
        return grid;
    }

    private void openEventCreateDialog() {
        EventCreateDialog dialog = new EventCreateDialog(existingEvents, Collections.<LeaderboardGroupDTO>emptyList(), sailingServiceWrite, stringMessages,
                new DialogCallback<EventDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final EventDTO newEvent) {
                        createEvent(newEvent);
                    }
                });
        dialog.show();
    }

    private void createEvent(final EventDTO newEvent) {
        sailingServiceWrite.createEvent(newEvent.getName(), newEvent.getDescription(), newEvent.startDate, newEvent.endDate,
                newEvent.getVenue().getName(), newEvent.isPublic, newEvent.getVenue().getCourseAreas(), newEvent.getOfficialWebsiteURL(), newEvent.getBaseURL(),
                newEvent.getSailorsInfoWebsiteURLs(), newEvent.getImages(),
                newEvent.getVideos(), newEvent.getLeaderboardGroupIds(), new AsyncCallback<EventDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError(stringMessages.errorTryingToCreateNewEvent(newEvent.getName(), t.getMessage()));
                    }

                    @Override
                    public void onSuccess(EventDTO newEvent) {
                        existingEvents.add(newEvent);
                        presenter.getEventsRefresher().updateAndCallFillForAll(existingEvents,
                                StructureImportManagementPanel.this.getEventsDisplayer());
                        sailingEventsListBox.addItem(newEvent.getName());
                        sailingEventsListBox.setSelectedIndex(sailingEventsListBox.getItemCount() - 1);
                    }
                });
    }

    private void listRegattasAndTheirStructures() {
        final String jsonURL;
        if (eventIDTextBox.getValue() == null || eventIDTextBox.getValue().length() == 0) {
            jsonURL = jsonURLTextBox.getValue();
        } else {
            jsonURL = SwissTimingEventIdUrlUtil.getUrlFromEventId(eventIDTextBox.getValue());
        }
        if (jsonURL == null || jsonURL.length() == 0) {
            errorReporter.reportError(stringMessages.pleaseEnterNonEmptyUrl());
        } else {
            busyIndicator.setBusy(true);
            sailingServiceWrite.getManage2SailRegattas(jsonURL, new AsyncCallback<Iterable<RegattaDTO>>() {
                @Override
                public void onFailure(Throwable caught) {
                    busyIndicator.setBusy(false);
                    errorReporter.reportError(stringMessages.errorLoadingRegattas(jsonURL, caught.getMessage()));
                }

                @Override
                public void onSuccess(Iterable<RegattaDTO> regattas) {
                    busyIndicator.setBusy(false);
                    editSeriesPanel.clear();
                    fillRegattas(regattas);
                    updateImportRegattasButtonEnabledness();
                    createEventSelectionAndCreateEventButtonUI();
                    editSeriesPanel.add(regattaStructureGrid);
                    updateRegattaStructureGrid();
                }
            });
        }
    }

    private void updateImportRegattasButtonEnabledness() {
        importDetailsButton.setEnabled(!regattaListComposite.getSelectedRegattas().isEmpty() &&
                getSelectedEvent() != null);
    }

    private void fillRegattas(Iterable<RegattaDTO> regattasFromXRR) {
        regattaStructures.clear();
        regattaDefaultsPerStructure.clear();
        for (RegattaDTO regattaFromXRR : regattasFromXRR) {
            RegattaStructure regattaStructure = new RegattaStructure(regattaFromXRR);
            if (!regattaDefaultsPerStructure.containsKey(regattaStructure)) {
                RegattaDTO defaultsForRegattasWithStructure = createRegattaDefaults(regattaFromXRR);
                regattaDefaultsPerStructure.put(regattaStructure, defaultsForRegattasWithStructure);
            }
            regattaStructures.put(regattaFromXRR, regattaStructure);
        }
        regattaListComposite.fillRegattas((List<RegattaDTO>) regattasFromXRR);
    }

    /**
     * Creates a new basically empty default regatta structure description object; this can be used as
     * an initial template that a user can modify as needed before instantiating the template for a set
     * of regattas.
     */
    private RegattaDTO createRegattaDefaults(RegattaDTO regatta) {
        RegattaDTO result = new RegattaDTO("Default", ScoringSchemeType.LOW_POINT);
        result.boatClass = new BoatClassDTO(BoatClassDTO.DEFAULT_NAME, /* hull length */ new MeterDistance(5), /* hull beam */ new MeterDistance(1.8));
        result.series = regatta.series;
        result.rankingMetricType = RankingMetrics.ONE_DESIGN;
        return result;
    }

    /**
     * Updates the grid holding the regatta structure descriptions with the corresponding "edit" buttons
     */
    private void updateRegattaStructureGrid() {
        while (regattaStructureGrid.getRowCount() > 0) {
            regattaStructureGrid.removeRow(0);
        }
        final List<RegattaDTO> originalXRRImportedRegattasToConsider;
        if (regattaListComposite.getSelectedRegattas().isEmpty()) {
            originalXRRImportedRegattasToConsider = regattaListComposite.getAllRegattas();
        } else {
            originalXRRImportedRegattasToConsider = regattaListComposite.getSelectedRegattas();
        }
        final Set<RegattaStructure> structures = new HashSet<>();
        for (RegattaDTO originalXRRImportedRegatta : originalXRRImportedRegattasToConsider) {
            structures.add(regattaStructures.get(originalXRRImportedRegatta));
        }
        int i = 0;
        for (RegattaStructure regattaStructure : structures) {
            final int row = i;
            updateRegattaStructureGridRow(row, regattaStructure);
            i++;
        }
    }

    /**
     * Updates a single row in the {@link #regattaStructureGrid}
     */
    private void updateRegattaStructureGridRow(final int row, final RegattaStructure regattaStructure) {
        Button editBtn = new Button(stringMessages.editSeries());
        editBtn.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                editRegattaDefaults(row, regattaStructure);
            }
        });
        regattaStructureGrid.setWidget(row, 0, new Label(regattaStructure.toString()));
        regattaStructureGrid.setWidget(row, 1, editBtn);
    }

    private void editRegattaDefaults(final int row, final RegattaStructure regattaStructure) {
        List<EventDTO> existingEvents = new ArrayList<EventDTO>();
        EventDTO selectedEvent = getSelectedEvent();
        if (selectedEvent != null) {
            existingEvents.add(getSelectedEvent());
        }
        DefaultRegattaCreateDialog dialog = new DefaultRegattaCreateDialog(existingEvents, regattaDefaultsPerStructure.get(regattaStructure),
                sailingServiceWrite, presenter.getUserService(), errorReporter, stringMessages, new DialogCallback<EventAndRegattaDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final EventAndRegattaDTO newRegattaWithEvent) {
                        final RegattaDTO newRegattaCreationDefaults = newRegattaWithEvent.getRegatta();
                        RegattaStructure newStructureEquivalenceClass = new RegattaStructure(newRegattaCreationDefaults);
                        final RegattaDTO replaced = regattaDefaultsPerStructure.put(newStructureEquivalenceClass, newRegattaCreationDefaults);
                        if (!regattaStructure.equals(newStructureEquivalenceClass)) {
                            // the creation defaults (probably particularly the fleet structure) was changed "incompatibly";
                            // if it equals another existing structure, use the creation defaults just edited for both and
                            // remove the grid line; otherwise replace the grid line;
                            // in any case update the StructureImportListComposite to reflect the changed structure
                            Map<RegattaDTO, RegattaStructure> updatesToPerform = new HashMap<>();
                            for (Entry<RegattaDTO, RegattaStructure> e : regattaStructures.entrySet()) {
                                if (e.getValue().equals(regattaStructure)) {
                                    // found a regatta that referenced the old structure which is now obsolete; let the XRR-imported
                                    // regatta point to the new structure
                                    updatesToPerform.put(e.getKey(), newStructureEquivalenceClass);
                                }
                            }
                            regattaStructures.putAll(updatesToPerform); // let original XRR-imported regattas point to their new structure
                            regattaDefaultsPerStructure.remove(regattaStructure);
                            regattaListComposite.fillRegattas(regattaStructures.keySet());
                            if (replaced != null) {
                                // requires a re-build of the grid because we don't know the other row to update/remove
                                updateRegattaStructureGrid();
                            } else {
                                updateRegattaStructureGridRow(row, newStructureEquivalenceClass);
                            }
                        }
                    }
                });
        dialog.ensureDebugId("DefaultRegattaCreateDialog");
        dialog.show();
    }

    private EventDTO getSelectedEvent() {
        EventDTO result = null;
        int selIndex = sailingEventsListBox.getSelectedIndex();
        if (selIndex > 0) { // the zero index represents the 'no selection' text
            String itemValue = sailingEventsListBox.getValue(selIndex);
            for (EventDTO eventDTO : existingEvents) {
                if (eventDTO.getName().equals(itemValue)) {
                    result = eventDTO;
                    break;
                }
            }
        }
        return result;
    }

    private void createRegattas(final Iterable<RegattaDTO> selectedOriginalRegattasFromXRR, EventDTO newEvent) {
        final List<RegattaDTO> regattaConfigurationsToCreate = new ArrayList<>();
        for (RegattaDTO originalRegattaFromXRR : selectedOriginalRegattasFromXRR) {
            RegattaDTO cloneFromDefaults = new RegattaDTO(regattaDefaultsPerStructure.get(regattaStructures.get(originalRegattaFromXRR)));
            cloneFromDefaults.setName(originalRegattaFromXRR.getName());
            cloneFromDefaults.boatClass = originalRegattaFromXRR.boatClass;
            // copy the race columns from original to clone of template
            Iterator<SeriesDTO> originalSeriesIter = originalRegattaFromXRR.series.iterator();
            Iterator<SeriesDTO> cloneFromDefaultsSeriesIter = cloneFromDefaults.series.iterator();
            while (originalSeriesIter.hasNext() && cloneFromDefaultsSeriesIter.hasNext()) {
                SeriesDTO originalSeries = originalSeriesIter.next();
                SeriesDTO cloneFromDefaultsSeries = cloneFromDefaultsSeriesIter.next();
                cloneFromDefaultsSeries.setRaceColumns(originalSeries.getRaceColumns());
            }
            regattaConfigurationsToCreate.add(cloneFromDefaults);
        }
        sailingServiceWrite.createRegattaStructure(regattaConfigurationsToCreate, newEvent,
                new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError(stringMessages.errorAddingResultImportUrl(caught.getMessage()));
            }

            @Override
            public void onSuccess(Void result) {
                presenter.getRegattasRefresher().reloadAndCallFillAll();
                Notification.notify(stringMessages.successfullyCreatedRegattas(), NotificationType.SUCCESS);
            }
        });
    }

    @Override
    public RegattaStructure getRegattaStructure(RegattaDTO regatta) {
        return regattaStructures.get(regatta);
    }
    
    public Displayer<EventDTO> getEventsDisplayer() {
        return eventsDisplayer;
    }

    private void fillEvents(Iterable<EventDTO> result) {
        if (sailingEventsListBox != null) { // is initialized only once a regatta structure has been loaded
            sailingEventsListBox.clear();
            existingEvents = new ArrayList<EventDTO>();
            Util.filter(result, e->presenter.getUserService().hasPermission(e, DefaultActions.UPDATE)).forEach(existingEvents::add);
            Collections.sort(existingEvents, new Comparator<EventDTO>() {
                private final NaturalComparator comp = new NaturalComparator();
                @Override
                public int compare(EventDTO o1, EventDTO o2) {
                    return comp.compare(o1.getName(), o2.getName());
                }
            });
            sailingEventsListBox.addItem(stringMessages.selectSailingEvent());
            for (EventDTO event : existingEvents) {
                sailingEventsListBox.addItem(event.getName());
            }
        }
    }
}
