package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RegattasDisplayer;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.perspective.ComponentContext;

/**
 * Shows the currently tracked events/races in a table. Updated if subscribed as an {@link RegattasDisplayer}, e.g., with
 * the {@link AdminConsoleEntryPoint}.
 */
public class TrackedRacesListComposite extends AbstractTrackedRacesListComposite {
    final Set<TrackedRaceChangedListener> raceIsTrackedRaceChangeListener;
    private Button btnUntrack;
    private Button btnRemoveRace;
    private Button btnSetDelayToLive;
    private Button btnExport;
    private ExportPopup exportPopup;
    private boolean actionButtonsEnabled;

    public TrackedRacesListComposite(Component<?> parent, ComponentContext<?> context,
            final SailingServiceAsync sailingService,
            final ErrorReporter errorReporter,
            final RegattaRefresher regattaRefresher, final StringMessages stringMessages, boolean hasMultiSelection, boolean actionButtonsEnabled) {
        super(parent, context, sailingService, errorReporter, regattaRefresher, stringMessages, hasMultiSelection);
        this.raceIsTrackedRaceChangeListener = new HashSet<TrackedRaceChangedListener>();
        this.actionButtonsEnabled = actionButtonsEnabled;
        createUI();
    }

    private void showSetDelayToLiveDialog() {
        TrackedRacesSettings settings = new TrackedRacesSettings();
        settings.setDelayToLiveInSeconds(DEFAULT_LIVE_DELAY_IN_MILLISECONDS);
        
        SettingsDialog<TrackedRacesSettings> settingsDialog = new SettingsDialog<TrackedRacesSettings>(this, stringMessages);
        settingsDialog.show();
    }
    
    public void addTrackedRaceChangeListener(TrackedRaceChangedListener listener) {
        this.raceIsTrackedRaceChangeListener.add(listener);
    }

    void stopTrackingRaces(final Iterable<RaceDTO> races) {
        final List<RegattaAndRaceIdentifier> racesToStopTracking = new ArrayList<RegattaAndRaceIdentifier>();
        for (RaceDTO race : races) {
            if (race.isTracked) {
                racesToStopTracking.add(race.getRaceIdentifier());
            }
        }
        sailingService.stopTrackingRaces(racesToStopTracking, new MarkedAsyncCallback<Void>(
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Exception trying to stop tracking races " + races + ": " + caught.getMessage());
                    }
        
                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.fillRegattas();
                        for (TrackedRaceChangedListener listener : raceIsTrackedRaceChangeListener) {
                            listener.racesStoppedTracking(racesToStopTracking);
                        }
                    }
                }));
    }

    private void removeAndUntrackRaces(final Iterable<RaceDTO> races) {
        final List<RegattaNameAndRaceName> regattaNamesAndRaceNames = new ArrayList<RegattaNameAndRaceName>();
        for (RaceDTO race : races) {
            regattaNamesAndRaceNames.add((RegattaNameAndRaceName) race.getRaceIdentifier());
        }
        sailingService.removeAndUntrackRaces(regattaNamesAndRaceNames, new MarkedAsyncCallback<Void>(
                new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Exception trying to remove races " + regattaNamesAndRaceNames +
                                ": " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.fillRegattas();
                        for (TrackedRaceChangedListener listener : raceIsTrackedRaceChangeListener) {
                            listener.racesRemoved(regattaNamesAndRaceNames);
                        }
                    }
                }));
    }

    @Override
    protected void addControlButtons(HorizontalPanel trackedRacesButtonPanel) {
        if(actionButtonsEnabled) {
            btnRemoveRace = new Button(stringMessages.remove());
            btnRemoveRace.ensureDebugId("RemoveRaceButton");
            btnRemoveRace.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    removeAndUntrackRaces(refreshableSelectionModel.getSelectedSet());
                }
            });
            btnRemoveRace.setEnabled(false);
            trackedRacesButtonPanel.add(btnRemoveRace);
            
            btnUntrack = new Button(stringMessages.stopTracking());
            btnUntrack.ensureDebugId("StopTrackingButton");
            btnUntrack.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent click) {
                    stopTrackingRaces(refreshableSelectionModel.getSelectedSet());
                }
            });
            btnUntrack.setEnabled(false);
            trackedRacesButtonPanel.add(btnUntrack);
            
            btnSetDelayToLive = new Button(stringMessages.setDelayToLive() + "...");
            btnSetDelayToLive.ensureDebugId("SetDelayToLiveButton");
            btnSetDelayToLive.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showSetDelayToLiveDialog();
                }
            });
            trackedRacesButtonPanel.add(btnSetDelayToLive);

            exportPopup = new ExportPopup(stringMessages);
            btnExport = new Button(stringMessages.export());
            btnExport.ensureDebugId("ExportButton");
            btnExport.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    exportPopup.center(new ArrayList<>(refreshableSelectionModel.getSelectedSet()));
                }
            });
            btnExport.setEnabled(false);
            trackedRacesButtonPanel.add(btnExport);
        }
    }

    @Override
    protected void makeControlsReactToSelectionChange(Set<RaceDTO> selectedRaces) {
        if (actionButtonsEnabled) {
            if (selectedRaces.isEmpty()) {
                btnRemoveRace.setEnabled(false);
                btnUntrack.setEnabled(false);
                btnExport.setEnabled(false);
            } else {
                btnRemoveRace.setEnabled(true);
                btnUntrack.setEnabled(true);
                btnExport.setEnabled(true);
            }
        }
    }

    @Override
    protected void makeControlsReactToFillRegattas(Iterable<RegattaDTO> regattas) {
        if(actionButtonsEnabled) {
            if (Util.isEmpty(regattas)) {
                btnUntrack.setVisible(false);
                btnRemoveRace.setVisible(false);
                btnSetDelayToLive.setVisible(false);
                btnExport.setVisible(false);
            } else {
                btnUntrack.setVisible(true);
                btnUntrack.setEnabled(false);
                btnRemoveRace.setVisible(true);
                btnRemoveRace.setEnabled(false);
                btnSetDelayToLive.setVisible(true);
                btnExport.setVisible(true);
            }
        }
    }

    @Override
    public String getDependentCssClassName() {
        return "trackedRacesListComposite";
    }

    @Override
    public TrackedRacesSettings getSettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getId() {
        return "TrackedRacesListComposite";
    }

    
}
