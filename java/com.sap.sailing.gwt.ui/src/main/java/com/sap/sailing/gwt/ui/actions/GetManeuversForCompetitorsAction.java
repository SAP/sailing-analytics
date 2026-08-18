package com.sap.sailing.gwt.ui.actions;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.ManeuverDTO;
import com.sap.sse.common.TimeRange;
import com.sap.sse.gwt.client.async.AsyncAction;

/**
 * {@link AsyncAction Asynchronous action} to load {@link ManeuverDTO maneuver information} for a number of
 * {@link CompetitorWithBoatDTO competitors} within the respectively associated {@link TimeRange time ranges}.
 */
public class GetManeuversForCompetitorsAction implements AsyncAction<Map<String, List<ManeuverDTO>>> {

    private final SailingServiceAsync sailingService;
    private final RegattaAndRaceIdentifier raceIdentifier;
    private final Map<String, TimeRange> competitorIdsAsStringsAndTimeRanges;

    public GetManeuversForCompetitorsAction(final SailingServiceAsync sailingService,
            final RegattaAndRaceIdentifier raceIdentifier,
            final Map<String, TimeRange> competitorIdsAsStringsAndTimeRanges) {
        this.sailingService = sailingService;
        this.raceIdentifier = raceIdentifier;
        this.competitorIdsAsStringsAndTimeRanges = competitorIdsAsStringsAndTimeRanges;
    }

    @Override
    public void execute(final AsyncCallback<Map<String, List<ManeuverDTO>>> callback) {
        sailingService.getManeuvers(raceIdentifier, competitorIdsAsStringsAndTimeRanges, callback);
    }

}
