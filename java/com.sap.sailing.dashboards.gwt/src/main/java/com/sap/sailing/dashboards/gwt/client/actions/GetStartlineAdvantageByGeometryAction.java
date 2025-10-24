package com.sap.sailing.dashboards.gwt.client.actions;

import java.util.UUID;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage.StartlineAdvantageByGeometryCalculator;
import com.sap.sailing.dashboards.gwt.shared.StartlineAdvantageType;
import com.sap.sailing.dashboards.gwt.shared.dispatch.DashboardDispatchContext;
import com.sap.sailing.dashboards.gwt.shared.dispatch.RequiresLiveRaceAndCachesMovingAverageAction;
import com.sap.sailing.dashboards.gwt.shared.dto.StartLineAdvantageDTO;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

/**
 * @author Alexander Ries (D062114)
 *
 */
public class GetStartlineAdvantageByGeometryAction extends RequiresLiveRaceAndCachesMovingAverageAction<StartLineAdvantageDTO> {

    public GetStartlineAdvantageByGeometryAction() {}

    public GetStartlineAdvantageByGeometryAction(String leaderboardName) {
        super(leaderboardName);
    }
    
    private static final String MOVING_AVERAGE_CACHE_KEY = UUID.randomUUID().toString();

    @Override
    @GwtIncompatible
    public StartLineAdvantageDTO execute(DashboardDispatchContext dashboardDispatchContext) throws DispatchException {
        StartLineAdvantageDTO result = new StartLineAdvantageDTO();
        Double startlineAdvantageByGeometry = null;
        TrackedRace liveRace = super.getLiveRace(dashboardDispatchContext);
        if (liveRace != null) {
            startlineAdvantageByGeometry = StartlineAdvantageByGeometryCalculator.calculateStartlineAdvantageByGeometry(liveRace);
        }
        result.startLineAdvantageType = StartlineAdvantageType.GEOMETRIC;
        result.startLineAdvantage = startlineAdvantageByGeometry;
        Double average = null;
        if (startlineAdvantageByGeometry != null) {
            super.addValueToMovingAverage(startlineAdvantageByGeometry.doubleValue(), dashboardDispatchContext.getMovingAveragesCache());
            average = dashboardDispatchContext.getMovingAveragesCache().getValueForKey(uniqueMovingAverageCacheKey());
        }
        result.average = average;
        return result;
    }

    @Override
    protected String uniqueMovingAverageCacheKey() {
        return MOVING_AVERAGE_CACHE_KEY;
    }
}
