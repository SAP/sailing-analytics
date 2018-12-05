package com.sap.sailing.domain.tracking;

import java.util.Optional;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sse.util.ThreadLocalTransporter;

/**
 * There are cases where extra work needs to be done when creating {@link TrackedRace TrackedRaces}. Due to the fact
 * that several trackers do not know race Ids or any other specific information before receiving signals to create
 * {@link TrackedRace TrackedRaces} which can in fact occur asynchronously. In this case, extra work (e.g. security
 * checks) can not be done in the direct user interaction. This interface allows to implement custom generic logic to
 * hook into {@link TrackedRace} creation on a {@link TrackedRegatta}.
 */
public interface RaceTrackingHandler {
    DynamicTrackedRace createTrackedRace(TrackedRegatta trackedRegatta, RaceDefinition raceDefinition,
            Iterable<Sideline> sidelines, WindStore windStore, long delayToLiveInMillis,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useMarkPassingCalculator,
            RaceLogResolver raceLogResolver, Optional<ThreadLocalTransporter> threadLocalTransporter);

    public class DefaultRaceTrackingHandler implements RaceTrackingHandler {
        @Override
        public DynamicTrackedRace createTrackedRace(TrackedRegatta trackedRegatta, RaceDefinition raceDefinition,
                Iterable<Sideline> sidelines, WindStore windStore, long delayToLiveInMillis,
                long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
                DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useMarkPassingCalculator,
                RaceLogResolver raceLogResolver, Optional<ThreadLocalTransporter> threadLocalTransporter) {
            return trackedRegatta.createTrackedRace(raceDefinition, sidelines, windStore, delayToLiveInMillis,
                    millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed, raceDefinitionSetToUpdate,
                    useMarkPassingCalculator, raceLogResolver, threadLocalTransporter);
        }
    }
}
