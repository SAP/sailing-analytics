package com.sap.sailing.server.trackfiles.test;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintRegistry;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;

public class DynamicTrackedRaceWithMarkPassingCalculator extends DynamicTrackedRaceImpl {
    private static final long serialVersionUID = -8076705893930566222L;

    public DynamicTrackedRaceWithMarkPassingCalculator(TrackedRegatta trackedRegatta, RaceDefinition race, Iterable<Sideline> sidelines,
            WindStore windStore, long delayToLiveInMillis, long millisecondsOverWhichToAverageWind,
            long millisecondsOverWhichToAverageSpeed, boolean useInternalMarkPassingAlgorithm,
            RankingMetricConstructor rankingMetricConstructor, RaceLogAndTrackedRaceResolver raceLogResolver, TrackingConnectorInfo trackingConnectorInfo,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry, ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry) {
        super(trackedRegatta, race, sidelines, windStore, delayToLiveInMillis, millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed, useInternalMarkPassingAlgorithm, rankingMetricConstructor, raceLogResolver, trackingConnectorInfo, markPassingRaceFingerprintRegistry, maneuverRaceFingerprintRegistry );
    }

    public MarkPassingCalculator getMarkPassingCalculator() {
        return markPassingCalculator;
    }
}
