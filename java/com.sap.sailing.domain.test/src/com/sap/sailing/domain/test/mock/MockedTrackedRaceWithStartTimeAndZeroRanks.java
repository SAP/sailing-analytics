package com.sap.sailing.domain.test.mock;

import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.leaderboard.Leaderboard.RankComparable;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Returns 0 for {@link #getRank(Competitor)} and {@link #getRank(Competitor, TimePoint)} in all cases. Still, the competitor
 * set participating in this race is maintained. This can be used, e.g., to mock a race that hasn't started yet and therefore
 * the competitors have no rank yet.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MockedTrackedRaceWithStartTimeAndZeroRanks extends MockedTrackedRaceWithStartTimeAndRanks {
    private static final long serialVersionUID = 7726627832998127727L;

    protected MockedTrackedRaceWithStartTimeAndZeroRanks(TimePoint startTime,
            List<Competitor> competitors, Regatta regatta) {
        super(startTime, competitors, regatta);
    }

    public MockedTrackedRaceWithStartTimeAndZeroRanks(TimePoint startTime, List<Competitor> competitors) {
        super(startTime, competitors);
    }

    @Override
    public Pair<Integer, RankComparable<?>> getRank(Competitor competitor) throws NoWindException {
        return new Pair<>(0,null);
    }

    @Override
    public Pair<Integer, RankComparable<?>> getRank(Competitor competitor, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return new Pair<>(0,null);
    }

}
