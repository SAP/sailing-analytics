package com.sap.sailing.polars.datamining.data.impl;

import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.datamining.impl.data.TrackedRaceWithContext;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.polars.datamining.data.HasCompetitorPolarContext;
import com.sap.sailing.polars.datamining.data.HasFleetPolarContext;
import com.sap.sailing.polars.datamining.data.HasGPSFixPolarContext;
import com.sap.sailing.polars.datamining.data.HasLeaderboardPolarContext;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sailing.polars.datamining.shared.PolarStatistic;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

public class GPSFixWithPolarContext implements HasGPSFixPolarContext {

    private final GPSFixMoving fix;
    private final TrackedRace trackedRace;
    private final ClusterGroup<Speed> windSpeedRangeGroup;
    private final Competitor competitor;
    private final PolarDataMiningSettings settings;
    private final HasCompetitorPolarContext competitorPolarContext;
    private final WindWithConfidence<Pair<Position, TimePoint>> wind;

    public GPSFixWithPolarContext(GPSFixMoving fix, TrackedRace trackedRace, ClusterGroup<Speed> windSpeedRangeGroup, Competitor competitor,
            PolarDataMiningSettings settings, WindWithConfidence<Pair<Position, TimePoint>> wind, HasCompetitorPolarContext competitorPolarContext) {
        this.fix = fix;
        this.trackedRace = trackedRace;
        this.windSpeedRangeGroup = windSpeedRangeGroup;
        this.competitor = competitor;
        this.settings = settings;
        this.competitorPolarContext = competitorPolarContext;
        this.wind = wind;
    }

    @Override
    public HasTrackedRaceContext getTrackedRaceContext() {
        final HasFleetPolarContext fleetPolarContext = getCompetitorPolarContext().getLegPolarContext().getFleetPolarContext();
        final RaceColumn raceColumn = fleetPolarContext.getRaceColumn();
        final HasLeaderboardPolarContext leaderboardPolarContext = fleetPolarContext.getRaceColumnPolarContext().getLeaderboardPolarContext();
        final Leaderboard leaderboard = leaderboardPolarContext.getLeaderboard();
        final HasLeaderboardContext leaderboardContext = new HasLeaderboardContext() {
            @Override
            public HasLeaderboardGroupContext getLeaderboardGroupContext() {
                return leaderboardPolarContext.getLeaderboardGroupContext();
            }

            @Override
            public Leaderboard getLeaderboard() {
                return leaderboard;
            }
        };
        return new TrackedRaceWithContext(leaderboardContext, trackedRace.getTrackedRegatta().getRegatta(), raceColumn,
                fleetPolarContext.getFleet(), trackedRace);
    }

    @Override
    public ClusterDTO getWindSpeedRange() {
        String signifier;
        if (wind == null || wind.getObject() == null) {
            signifier = "null";
        } else {
            Cluster<Speed> cluster = windSpeedRangeGroup.getClusterFor(wind.getObject());
            signifier = cluster == null ? "null" : cluster.toString();
        }
        return new ClusterDTO(signifier);
    }

    @Override
    public PolarStatistic getPolarStatistics() {
        return new PolarStatisticImpl(trackedRace, competitor, fix, settings, wind.getObject());
    }

    @Override
    public HasCompetitorPolarContext getCompetitorPolarContext() {
        return competitorPolarContext;
    }

    @Override
    public boolean isFoiling() {
        final boolean result;
        final BravoFixTrack<Competitor> competitorBravoFixTrack = trackedRace.getSensorTrack(competitor, BravoFixTrack.TRACK_NAME);
        if (competitorBravoFixTrack != null) {
            result = competitorBravoFixTrack.isFoiling(fix.getTimePoint());
        } else {
            result = false;
        }
        return result;
    }
}
