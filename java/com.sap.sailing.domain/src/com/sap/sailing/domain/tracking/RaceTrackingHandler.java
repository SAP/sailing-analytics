package com.sap.sailing.domain.tracking;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Optional;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
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
            RaceLogAndTrackedRaceResolver raceLogResolver, Optional<ThreadLocalTransporter> threadLocalTransporter,
            TrackingConnectorInfo trackingConnectorInfo, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry);

    DynamicCompetitor getOrCreateCompetitor(CompetitorAndBoatStore competitorAndBoatStore, Serializable competitorId,
            String name, String shortName, Color displayColor, String email, URI flagImageURI, DynamicTeam team,
            Double timeOnTimeFactor, Duration timeOnDistanceAllowancePerNauticalMile, String searchTag);

    RaceDefinition createRaceDefinition(Regatta regatta, String name, Course course, BoatClass boatClass,
            Map<Competitor, Boat> competitorsAndTheirBoats, Serializable id);

    DynamicCompetitorWithBoat getOrCreateCompetitorWithBoat(CompetitorAndBoatStore competitorStore,
            Serializable competitorId, String name, String shortName, Color displayColor, String email,
            URI flagImageURI, DynamicTeam team, Double timeOnTimeFactor,
            Duration timeOnDistanceAllowancePerNauticalMile, String searchTag, DynamicBoat boat);

    DynamicBoat getOrCreateBoat(CompetitorAndBoatStore competitorAndBoatStore, Serializable id, String name,
            BoatClass boatClass, String sailId, Color color);

    public class DefaultRaceTrackingHandler implements RaceTrackingHandler {
        @Override
        public DynamicTrackedRace createTrackedRace(TrackedRegatta trackedRegatta, RaceDefinition raceDefinition,
                Iterable<Sideline> sidelines, WindStore windStore, long delayToLiveInMillis,
                long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
                DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useMarkPassingCalculator,
                RaceLogAndTrackedRaceResolver raceLogResolver, Optional<ThreadLocalTransporter> threadLocalTransporter,
                TrackingConnectorInfo trackingConnectorInfo, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
            return trackedRegatta.createTrackedRace(raceDefinition, sidelines, windStore, delayToLiveInMillis,
                    millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed, raceDefinitionSetToUpdate,
                    useMarkPassingCalculator, raceLogResolver, threadLocalTransporter, trackingConnectorInfo, markPassingRaceFingerprintRegistry);
        }

        @Override
        public RaceDefinition createRaceDefinition(Regatta regatta, String name, Course course,
                BoatClass boatClass, Map<Competitor, Boat> competitorsAndTheirBoats, Serializable id) {
            return new RaceDefinitionImpl(name, course, boatClass, competitorsAndTheirBoats, id);
        }

        @Override
        public DynamicCompetitor getOrCreateCompetitor(CompetitorAndBoatStore competitorStore,
                Serializable competitorId, String name, String shortName,
                Color displayColor, String email, URI flagImageURI, DynamicTeam team, Double timeOnTimeFactor,
                Duration timeOnDistanceAllowancePerNauticalMile, String searchTag) {
            return competitorStore.getOrCreateCompetitor(competitorId, name, shortName, displayColor, email,
                    flagImageURI, team, timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMile,
                    searchTag, /* storePersistently */ true);
        }

        @Override
        public DynamicCompetitorWithBoat getOrCreateCompetitorWithBoat(CompetitorAndBoatStore competitorStore,
                Serializable competitorId, String name, String shortName, Color displayColor, String email,
                URI flagImageURI, DynamicTeam team, Double timeOnTimeFactor,
                Duration timeOnDistanceAllowancePerNauticalMile, String searchTag, DynamicBoat boat) {
            return competitorStore.getOrCreateCompetitorWithBoat(competitorId, name, shortName, displayColor, email,
                    flagImageURI, team, timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMile, searchTag, boat, /* storePersistently */ true);
        }

        @Override
        public DynamicBoat getOrCreateBoat(CompetitorAndBoatStore competitorAndBoatStore, Serializable id, String name,
                BoatClass boatClass, String sailId, Color color) {
            return competitorAndBoatStore.getOrCreateBoat(id, name, boatClass, sailId, color, /* storePersistently */ true);
        }
    }
}
