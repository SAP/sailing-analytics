package com.sap.sailing.gwt.ui.actions;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.shared.CompactBoatPositionsDTO;
import com.sap.sailing.gwt.ui.shared.CompactRaceMapDataDTO;
import com.sap.sailing.gwt.ui.shared.GPSFixDTOWithSpeedWindTackAndLegTypeIterable;
import com.sap.sailing.gwt.ui.shared.RaceMapDataDTO;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.async.AsyncAction;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.TimeRangeActionsExecutor;

/**
 * When it gets {@ink #dropped(AsyncActionsExecutor) dropped}, at least the
 * {@link SailingServiceAsync#getBoatPositions(RegattaAndRaceIdentifier, Map, Map, boolean, DetailType, String, String, UUID, AsyncCallback)
 * getBoatPositions} call must be executed, then through the {@link TimeRangeActionsExecutor}, with the callback
 * provided to the constructor.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class GetRaceMapDataAction extends AbstractGetMapRelatedDataAction<RaceMapDataDTO> implements AsyncAction<RaceMapDataDTO> {
    private final Map<String, CompetitorDTO> competitorsByIdAsString;
    private final Date date;
    private final LegIdentifier simulationLegIdentifier;
    private final byte[] md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID;
    private Date timeForEstimation;
    private boolean targetEstimationRequired;
    private final TimeRangeActionsExecutor<CompactBoatPositionsDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable, Pair<String, DetailType>> timeRangeActionsExecutor;
    private final GetBoatPositionsCallback getBoatPositionsCallback;

    public GetRaceMapDataAction(SailingServiceAsync sailingService,
            TimeRangeActionsExecutor<CompactBoatPositionsDTO, GPSFixDTOWithSpeedWindTackAndLegTypeIterable, Pair<String, DetailType>> timeRangeActionsExecutor,
            Map<String, CompetitorDTO> competitorsByIdAsString, RegattaAndRaceIdentifier raceIdentifier, Date date, Map<String, Date> from,
            Map<String, Date> to, boolean extrapolate, LegIdentifier simulationLegIdentifier,
            byte[] md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID, Date timeForEstimation,
            boolean targetEstimationRequired, DetailType detailType, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, GetBoatPositionsCallback getBoatPositionsCallback) {
        super(sailingService, raceIdentifier, from, to, extrapolate, detailType, leaderboardName, leaderboardGroupName, leaderboardGroupId);
        this.timeRangeActionsExecutor = timeRangeActionsExecutor;
        this.competitorsByIdAsString = competitorsByIdAsString;
        this.timeForEstimation = timeForEstimation;
        this.targetEstimationRequired = targetEstimationRequired;
        this.date = date;
        this.simulationLegIdentifier = simulationLegIdentifier;
        this.md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID = md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID;
        this.getBoatPositionsCallback = getBoatPositionsCallback;
    }
    
    @Override
    public void execute(final AsyncCallback<RaceMapDataDTO> callback) {
        Map<String, Date> fromByCompetitorIdAsString = getFromByCompetitorIdAsString();
        Map<String, Date> toByCompetitorIdAsString = getToByCompetitorIdAsString();
        getSailingService().getRaceMapData(getRaceIdentifier(), date, fromByCompetitorIdAsString, toByCompetitorIdAsString, isExtrapolate(), simulationLegIdentifier,
                md5OfIdsAsStringOfCompetitorParticipatingInRaceInAlphanumericOrderOfTheirID, timeForEstimation, targetEstimationRequired, getDetailType(),
                getLeaderboardName(), getLeaderboardGroupName(), getLeaderboardGroupId(), 
                new AsyncCallback<CompactRaceMapDataDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }

                    @Override
                    public void onSuccess(CompactRaceMapDataDTO result) {
                        callback.onSuccess(result.getRaceMapDataDTO(competitorsByIdAsString));
                    }
                });
    }
    
    /**
     * When a request of this type has been dropped, a replacement request needs to be fired for the boat positions.
     * These have been expected by the caller to fill up the cache. However, this only has to happen for those
     * competitors whose cached fixes haven't been evicted in the meantime.
     */
    @Override
    public void dropped(AsyncActionsExecutor executor) {
        GWT.log("Executing getBoatPositions(...) call instead of a dropped GetRaceMapDataAction:\n"+getFromByCompetitorIdAsString()+"; "+getToByCompetitorIdAsString());
        // TODO bug5921: if we had the PositionRequest object at hand, we could check if the positions are still required or if they were marked as to be dropped in which case no request would have to be made;
        // We could then compute the getFrom() and getTo() from the PositionRequest which would filter those time ranges that will be dropped anyhow; if empty, no request will need to be sent anymore.
        timeRangeActionsExecutor.execute(new GetBoatPositionsAction(getSailingService(), getRaceIdentifier(),
                getFromByCompetitorIdAsString(), getToByCompetitorIdAsString(), isExtrapolate(), getDetailType(), getLeaderboardName(), getLeaderboardGroupName(), getLeaderboardGroupId()),
                getBoatPositionsCallback);
    }
}
