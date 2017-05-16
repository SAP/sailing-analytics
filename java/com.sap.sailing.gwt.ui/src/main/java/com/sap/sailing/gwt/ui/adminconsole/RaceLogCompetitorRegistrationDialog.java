package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithToolTipDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.gwt.ui.client.ParallelExecutionCallback;
import com.sap.sailing.gwt.ui.client.ParallelExecutionHolder;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class RaceLogCompetitorRegistrationDialog extends AbstractCompetitorRegistrationsDialog {

    private String fleetName;
    private String raceColumnName;
    private CheckBox competitorRegistrationInRaceLogCheckBox;
    private final Map<String, Set<CompetitorDTO>> fleetNameWithCompetitors;
    
    private static class Validator implements com.sap.sse.gwt.client.dialog.DataEntryDialog.Validator<Set<CompetitorDTO>> {
        private CheckBox competitorRegistrationInRaceLogCheckBox;
        private Map<String, Set<CompetitorDTO>> fleetsWithCompetitors;
        private final StringMessages stringMessages;
        
        public Validator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(Set<CompetitorDTO> valueToValidate) {
            String result = null;
            if (getCompetitorRegistrationInRaceLogCheckBox() != null && !getCompetitorRegistrationInRaceLogCheckBox().getValue()) {
                result = stringMessages.competitorRegistrationsOnRaceDisabled();
            } else {
                Set<CompetitorDTO> difference = new HashSet<>();
                for (Set<CompetitorDTO> competitors : fleetsWithCompetitors.values()) {
                    difference.addAll(intersection(valueToValidate, competitors));
                }
                if (!difference.isEmpty()) {
                    result = stringMessages.warningForDisabledCompetitors(createLineOfCompetitors(difference));
                }
            }
            return result;
        }
        
        private String createLineOfCompetitors(Set<CompetitorDTO> competitors) {
            StringBuilder lineOfCompetitors = new StringBuilder();
            if (!competitors.isEmpty()) {
                for (CompetitorDTO competitor : competitors) {
                    lineOfCompetitors.append(competitor.getName()).append(", ");
                }
                lineOfCompetitors.delete(lineOfCompetitors.length() - 2, lineOfCompetitors.length() - 1);
            }
            return lineOfCompetitors.toString();
        }
        
        private Set<CompetitorDTO> intersection(Set<CompetitorDTO> firstSet, Set<CompetitorDTO> secondSet) {
            Set<CompetitorDTO> result = new LinkedHashSet<>(firstSet);
            result.retainAll(secondSet);
            return result;
        }
        
        public CheckBox getCompetitorRegistrationInRaceLogCheckBox() {
            return competitorRegistrationInRaceLogCheckBox;
        }

        public void setCompetitorRegistrationInRaceLogCheckBox(CheckBox competitorRegistrationInRaceLogCheckBox) {
            this.competitorRegistrationInRaceLogCheckBox = competitorRegistrationInRaceLogCheckBox;
        }

        public void setFleetWithCompetitors(Map<String, Set<CompetitorDTO>> fleetNameWithCompetitors) {
            this.fleetsWithCompetitors = fleetNameWithCompetitors;
        }
    }

    public RaceLogCompetitorRegistrationDialog(String boatClass, SailingServiceAsync sailingService,
            StringMessages stringMessages, ErrorReporter errorReporter, boolean editable, String leaderboardName,
            String raceColumnName, String fleetName, List<FleetDTO> fleets,
            com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback<Set<CompetitorDTO>> callback) {
        this(sailingService, stringMessages, errorReporter, editable, callback, leaderboardName, boatClass,
                raceColumnName, fleetName, fleets, new Validator(stringMessages));
    }
    
    public RaceLogCompetitorRegistrationDialog(SailingServiceAsync sailingService, StringMessages stringMessages,
            ErrorReporter errorReporter, boolean editable,
            com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback<Set<CompetitorDTO>> callback,
            String leaderboardName, String boatClass, String raceColumnName, String fleetName, List<FleetDTO> fleets, Validator validator) {
        super(sailingService, stringMessages, errorReporter, editable, callback, leaderboardName, boatClass, validator);
        this.raceColumnName = raceColumnName;
        this.fleetName = fleetName;
        fleetNameWithCompetitors = findCompetitorsFromTheSameRaceColumn(fleets);
        competitorRegistrationInRaceLogCheckBox = createCheckbox(stringMessages.registerCompetitorsOnRace());
        validator.setCompetitorRegistrationInRaceLogCheckBox(competitorRegistrationInRaceLogCheckBox);
        validator.setFleetWithCompetitors(fleetNameWithCompetitors);
        setupCompetitorRegistationsOnRaceCheckbox();
    }

    /**
     * For all {@code fleets} passed and not equal to {@link #fleetName} retrieves the competitor-per-race registrations
     * for the {@link #raceColumnName}. 
     */
    private Map<String, Set<CompetitorDTO>> findCompetitorsFromTheSameRaceColumn(final List<FleetDTO> fleets) {
        final Map<String, Set<CompetitorDTO>> result = new HashMap<>();
        final Map<String, ParallelExecutionCallback<Collection<CompetitorDTO>>> callbacksForFleetNames = new HashMap<>();
        for (FleetDTO fleetDTO : fleets) {
            final String curFleetName = fleetDTO.getName();
            if (!curFleetName.equals(fleetName)) {
                callbacksForFleetNames.put(curFleetName, new ParallelExecutionCallback<Collection<CompetitorDTO>>() {
                    @Override
                    public void onSuccess(Collection<CompetitorDTO> competitorRegistrationsForRace) {
                        result.put(curFleetName, new HashSet<>(competitorRegistrationsForRace));
                        super.onSuccess(competitorRegistrationsForRace);
                    }
                });
            }
        }
        if (!callbacksForFleetNames.isEmpty()) {
            new ParallelExecutionHolder(callbacksForFleetNames.values().toArray(new ParallelExecutionCallback<?>[0])) {
                @Override
                protected void handleSuccess() {
                    // if the data was gained completely then gray out specific rows
                    grayOutRows();
                }

                @Override
                protected void handleFailure(Throwable t) {
                    errorReporter.reportError("Could not load already registered competitors: " + t.getMessage());
                }
            };
            for (final Entry<String, ParallelExecutionCallback<Collection<CompetitorDTO>>> fleetNameAndCallback : callbacksForFleetNames
                    .entrySet()) {
                sailingService.getCompetitorRegistrationsForRace(leaderboardName, raceColumnName,
                        fleetNameAndCallback.getKey(), fleetNameAndCallback.getValue());
            }
        }
        return result;
    }

    /**
     * Grays out rows with competitors from the same race column
     */
    private void grayOutRows() {
        List<CompetitorWithToolTipDTO> competitors = new ArrayList<>();
        for (Map.Entry<String, Set<CompetitorDTO>> entry : fleetNameWithCompetitors.entrySet()) {
            if (!entry.getKey().equals(fleetName)) {
                for (CompetitorDTO competitor : entry.getValue()) {
                    competitors.add(new CompetitorWithToolTipDTO(competitor, stringMessages
                            .competitorToolTipMessage(competitor.getName(), fleetName, entry.getKey(), raceColumnName)));
                }
            }
        }
        allCompetitorsTable.grayOutCompetitors(competitors);
        registeredCompetitorsTable.grayOutCompetitors(competitors);
    }

    @Override
    protected void setRegisteredCompetitors() {
        if (showOnlyCompetitorsOfLog()) {
            sailingService.getCompetitorRegistrationsInRaceLog(leaderboardName, raceColumnName, fleetName,
                    new AsyncCallback<Collection<CompetitorDTO>>() {
                        @Override
                        public void onSuccess(Collection<CompetitorDTO> registeredCompetitors) {
                            move(allCompetitorsTable, registeredCompetitorsTable, registeredCompetitors);
                            validateAndUpdate();
                        }

                        @Override
                        public void onFailure(Throwable reason) {
                            errorReporter.reportError("Could not load already registered competitors: "
                                    + reason.getMessage());
                        }
                    });
        } else {
            sailingService.getCompetitorRegistrationsForRace(leaderboardName, raceColumnName, fleetName,
                    new AsyncCallback<Collection<CompetitorDTO>>() {
                        @Override
                        public void onSuccess(Collection<CompetitorDTO> registeredCompetitors) {
                            move(allCompetitorsTable, registeredCompetitorsTable, registeredCompetitors);
                            validateAndUpdate();
                        }

                        @Override
                        public void onFailure(Throwable reason) {
                            errorReporter.reportError("Could not load already registered competitors: "
                                    + reason.getMessage());
                        }
                    });
        }
    }

    @Override
    public void addAdditionalWidgets(FlowPanel mainPanel) {
        mainPanel.add(competitorRegistrationInRaceLogCheckBox);
    }

    private void setupCompetitorRegistationsOnRaceCheckbox() {
        sailingService.areCompetitorRegistrationsEnabledForRace(leaderboardName, raceColumnName, fleetName,
                new AsyncCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean isEnabled) {
                        competitorRegistrationInRaceLogCheckBox.setValue(isEnabled);
                        if (isEnabled) {
                            activateRegistrationButtons();
                        } else {
                            deactivateRegistrationButtons(stringMessages.competitorRegistrationsOnRaceDisabled());
                        }
                    }

                    @Override
                    public void onFailure(Throwable reason) {
                        errorReporter.reportError("Could not load already registered competitors: "
                                + reason.getMessage());
                    }
                });
        competitorRegistrationInRaceLogCheckBox.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String title;
                final String message;
                if (competitorRegistrationInRaceLogCheckBox.getValue()) {
                    title = stringMessages.doYouWantToRegisterCompetitorsDirectlyOnTheRace();
                    message = stringMessages.warningDirectCompetitorRegistration();
                } else {
                    title = stringMessages.doYouWantToDisableCompetitorsRegistrationsOnTheRace();
                    message = stringMessages.warningRegattaCompetitorRegistration();
                }
                new DataEntryDialog<Void>(title, message, stringMessages.ok(), stringMessages.cancel(), null, false,
                        new DialogCallback<Void>() {
                            @Override
                            public void ok(Void editedObject) {
                                if (competitorRegistrationInRaceLogCheckBox.getValue()) {
                                    sailingService.enableCompetitorRegistrationsForRace(leaderboardName,
                                            raceColumnName, fleetName, new AsyncCallback<Void>() {
                                                @Override
                                                public void onSuccess(Void isEnabled) {
                                                    competitorRegistrationInRaceLogCheckBox.setValue(true);
                                                    activateRegistrationButtons();
                                                    refreshCompetitors();
                                                }

                                                @Override
                                                public void onFailure(Throwable reason) {
                                                    errorReporter
                                                            .reportError("Could not enable competitor registrations for race: "
                                                                    + reason.getMessage());
                                                }
                                            });

                                } else {
                                    sailingService.disableCompetitorRegistrationsForRace(leaderboardName,
                                            raceColumnName, fleetName, new AsyncCallback<Void>() {
                                                @Override
                                                public void onSuccess(Void isEnabled) {
                                                    competitorRegistrationInRaceLogCheckBox.setValue(false);
                                                    deactivateRegistrationButtons(stringMessages
                                                            .competitorRegistrationsOnRaceDisabled());
                                                    refreshCompetitors();
                                                }

                                                @Override
                                                public void onFailure(Throwable reason) {
                                                    errorReporter
                                                            .reportError("Could not deactivate competitor registrations for race: "
                                                                    + reason.getMessage());
                                                }
                                            });
                                }
                            }

                            @Override
                            public void cancel() {
                                competitorRegistrationInRaceLogCheckBox.setValue(!competitorRegistrationInRaceLogCheckBox.getValue());
                            }
                        }) {
                    @Override
                    protected Void getResult() {
                        return null;
                    }
                }.show();
            }
        });
    }

    @Override
    protected Set<CompetitorDTO> getResult() {
        if (competitorRegistrationInRaceLogCheckBox.getValue()) {
            return super.getResult();
        } else {
            return Collections.emptySet();
        }
    }
}
