package com.sap.sailing.polars.datamining.data.impl;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.polars.datamining.data.HasCompetitorPolarContext;
import com.sap.sailing.polars.datamining.data.HasGPSFixPolarContext;
import com.sap.sailing.polars.datamining.shared.PolarDataMiningSettings;
import com.sap.sailing.polars.datamining.shared.PolarStatistic;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

public class GPSFixWithPolarContext implements HasGPSFixPolarContext {

    private final GPSFixMoving fix;
    private final TrackedRace trackedRace;
    private final ClusterGroup<Speed> windSpeedRangeGroup;
    private final Competitor competitor;
    private final PolarDataMiningSettings settings;
    private final HasCompetitorPolarContext competitorPolarContext;
    private WindWithConfidence<Pair<Position, TimePoint>> wind;

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
    public ClusterDTO getWindSpeedRange() {
        return new ClusterDTO(windSpeedRangeGroup.getClusterFor(wind.getObject()).toString());
    }

    @Override
    public PolarStatistic getPolarStatistics() {
        return new PolarStatisticImpl(trackedRace, competitor, fix, settings, wind.getObject());
    }

    @Override
    public HasCompetitorPolarContext getCompetitorPolarContext() {
        return competitorPolarContext;
    }

}
