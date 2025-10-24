package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.dto.CourseAreaDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.domain.common.dto.RegattaCreationParametersDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

public class CreateRegattaCallback implements DialogCallback<RegattaDTO>{

    private final SailingServiceWriteAsync sailingServiceWrite;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final Iterable<EventDTO> existingEvents;
    private final Presenter presenter;

    public CreateRegattaCallback(StringMessages stringMessages, Presenter presenter, Iterable<EventDTO> existingEvents) {
        this.sailingServiceWrite = presenter.getSailingService();
        this.errorReporter = presenter.getErrorReporter();
        this.presenter = presenter;
        this.stringMessages = stringMessages;
        this.existingEvents = existingEvents;
    }

    @Override
    public void ok(RegattaDTO newRegatta) {
        createNewRegatta(newRegatta, existingEvents);
    }

    @Override
    public void cancel() {
    }

    private void createNewRegatta(final RegattaDTO newRegatta, final Iterable<EventDTO> existingEvents) {
        LinkedHashMap<String, SeriesCreationParametersDTO> seriesStructure = new LinkedHashMap<String, SeriesCreationParametersDTO>();
        for (SeriesDTO seriesDTO : newRegatta.series) {
            SeriesCreationParametersDTO seriesPair = new SeriesCreationParametersDTO(seriesDTO.getFleets(),
                    seriesDTO.isMedal(), seriesDTO.isFleetsCanRunInParallel(), seriesDTO.isStartsWithZeroScore(),
                    seriesDTO.isFirstColumnIsNonDiscardableCarryForward(), seriesDTO.getDiscardThresholds(),
                    seriesDTO.hasSplitFleetContiguousScoring(), seriesDTO.hasCrossFleetMergedRanking(), seriesDTO.getMaximumNumberOfDiscards(), seriesDTO.isOneAlwaysStaysOne());
            seriesStructure.put(seriesDTO.getName(), seriesPair);
        }
        sailingServiceWrite.createRegatta(newRegatta.getName(),
                newRegatta.boatClass == null ? null : newRegatta.boatClass.getName(),
                newRegatta.canBoatsOfCompetitorsChangePerRace, newRegatta.competitorRegistrationType,
                newRegatta.registrationLinkSecret, newRegatta.startDate, newRegatta.endDate,
                new RegattaCreationParametersDTO(seriesStructure), true, newRegatta.scoringScheme,
                Util.mapToArrayList(newRegatta.courseAreas, CourseAreaDTO::getId), newRegatta.buoyZoneRadiusInHullLengths,
                newRegatta.useStartTimeInference, newRegatta.controlTrackingFromStartAndFinishTimes,
                newRegatta.autoRestartTrackingUponCompetitorSetChange, newRegatta.rankingMetricType,
                new AsyncCallback<RegattaDTO>() {
            @Override
            public void onFailure(Throwable t) {
                errorReporter.reportError("Error trying to create new regatta " + newRegatta.getName() + ": " + t.getMessage());
            }

            @Override
            public void onSuccess(RegattaDTO regatta) {
                // if regatta creation was successful, add race columns as modeled in the creation dialog;
                // note that the SeriesCreationParametersDTO don't describe race columns.
                createDefaultRacesIfDefaultSeriesIsPresent(newRegatta);
                reloadRegattas();
                fillEvents(); // events have their associated regattas
                openCreateDefaultRegattaLeaderboardDialog(regatta, existingEvents);
            }
        });
    }

    private void createDefaultRacesIfDefaultSeriesIsPresent(final RegattaDTO newRegatta) {
        for (final SeriesDTO series: newRegatta.series) {
            if (series.getName().equals(LeaderboardNameConstants.DEFAULT_SERIES_NAME) && !series.getRaceColumns().isEmpty()) {
                final List<Pair<String, Integer>> raceColumnNamesToAddWithInsertIndex = new ArrayList<>();
                for (RaceColumnDTO newRaceColumn : series.getRaceColumns()) {
                    // We could use an index counter here because we're assuming that we're creating
                    // races starting at index 0. However, to make things concurrency-safe, we have to
                    // assume that while the regatta already exists on the server, some other activity
                    // may already have started to create races for it. Better safe than sorry, append
                    // at the end, using -1 as "insertIndex."
                    raceColumnNamesToAddWithInsertIndex.add(new Pair<>(newRaceColumn.getName(), -1));
                }
                sailingServiceWrite.addRaceColumnsToSeries(newRegatta.getRegattaIdentifier(), series.getName(), raceColumnNamesToAddWithInsertIndex,
                        new AsyncCallback<List<RaceColumnInSeriesDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to add race columns " + raceColumnNamesToAddWithInsertIndex
                                + " to series " + series.getName() + ": " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<RaceColumnInSeriesDTO> raceColumns) {
                        reloadRegattas();
                    }
                });
            }
        }
    }

    private void reloadLeaderboards() {
        if (presenter.getLeaderboardsRefresher() != null) {
            presenter.getLeaderboardsRefresher().reloadAndCallFillAll();
        }
    }

    private void reloadRegattas() {
        if (presenter.getRegattasRefresher() != null){
            presenter.getRegattasRefresher().reloadAndCallFillAll();
        }
    }

    private void fillEvents() {
        if (presenter.getEventsRefresher() != null) {
            presenter.getEventsRefresher().reloadAndCallFillAll();
        }
    }

    private void openCreateDefaultRegattaLeaderboardDialog(final RegattaDTO newRegatta, final Iterable<EventDTO> existingEvents) {
        CreateDefaultRegattaLeaderboardDialog dialog = new CreateDefaultRegattaLeaderboardDialog(sailingServiceWrite,
                stringMessages, errorReporter, newRegatta, new DialogCallback<RegattaName>() {
            @Override
                    public void ok(RegattaName regattaIdentifier) {
                        sailingServiceWrite.createRegattaLeaderboard(regattaIdentifier,
                                /* displayName */ null, new int[] {},
                                new AsyncCallback<StrippedLeaderboardDTO>() {
                    @Override
                    public void onFailure(Throwable t) {
                        errorReporter.reportError("Error trying to create default regatta leaderboard for " + newRegatta.getName()
                                + ": " + t.getMessage());
                    }

                    @Override
                    public void onSuccess(StrippedLeaderboardDTO result) {
                        if (!newRegatta.courseAreas.isEmpty()) {
                            // Show the event's leaderboard groups and allow the user to pick one to assign the regatta leaderboard to
                            final EventDTO event = getEventForCourseArea(existingEvents, newRegatta.courseAreas);
                            if (!event.getLeaderboardGroups().isEmpty()) {
                                openRegattaLeaderboardToLeaderboardGroupOfEventLinkingDialog(result, event);
                            }
                        }
                        reloadLeaderboards();
                    }

                });
            }

            @Override
            public void cancel() {
            }
        });
        dialog.ensureDebugId("CreateDefaultRegattaLeaderboardDialog");
        dialog.show();
    }


    /**
     * When a new regatta with a new regatta leaderboard has been created, the user will now be given the chance to link
     * the regatta leaderboard into a leaderboard group of the event out of which the regatta chose its default course area.
     *
     * @param newRegattaLeaderboard the new regatta leaderboard that the user may link now to a leaderboard group of an event
     * @param eventToLinkRegattaTo an event that has at least one {@link EventDTO#getLeaderboardGroups() leaderboard group}
     */
    private void openRegattaLeaderboardToLeaderboardGroupOfEventLinkingDialog(final StrippedLeaderboardDTO newRegattaLeaderboard, EventDTO eventToLinkRegattaTo) {
        LinkRegattaLeaderboardToLeaderboardGroupOfEventDialog dialog = new LinkRegattaLeaderboardToLeaderboardGroupOfEventDialog(sailingServiceWrite, stringMessages, errorReporter, newRegattaLeaderboard, eventToLinkRegattaTo,
                new DialogCallback<LeaderboardGroupDTO>() {
                    @Override
                    public void ok(final LeaderboardGroupDTO selectedLeaderboardGroup) {
                        final List<String> leaderboardNames = new ArrayList<>();
                        for (StrippedLeaderboardDTO leaderboard : selectedLeaderboardGroup.getLeaderboards()) {
                            leaderboardNames.add(leaderboard.getName());
                        }
                        leaderboardNames.add(newRegattaLeaderboard.getName());
                        sailingServiceWrite.updateLeaderboardGroup(selectedLeaderboardGroup.getId(),
                                selectedLeaderboardGroup.getName(), selectedLeaderboardGroup.getName(),
                                selectedLeaderboardGroup.getDescription(), selectedLeaderboardGroup.getDisplayName(),
                                leaderboardNames, selectedLeaderboardGroup.getOverallLeaderboardDiscardThresholds(),
                                selectedLeaderboardGroup.getOverallLeaderboardScoringSchemeType(),
                                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter
                                                .reportError(stringMessages.failedToLinkLeaderboardToLeaderboardGroup(
                                                        newRegattaLeaderboard.getName(),
                                                        selectedLeaderboardGroup.getName()));
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                    }
                                }));
                    }

                    @Override
                    public void cancel() {
                    }
        });
        dialog.ensureDebugId("LinkRegattaLeaderboardToLeaderboardGroupOfEventDialog");
        dialog.show();
    }

    private EventDTO getEventForCourseArea(final Iterable<EventDTO> existingEvents, final Iterable<CourseAreaDTO> courseAreas) {
        EventDTO result = null;
        eventLoop:
        for (final EventDTO event : existingEvents) {
            if (event.getVenue() != null) {
                if (Util.containsAny(event.getVenue().getCourseAreas(), courseAreas)) {
                    result = event;
                    break eventLoop;
                }
            }
        }
        return result;
    }
}
