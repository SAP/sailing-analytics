package com.sap.sailing.domain.racelogtracking.test.impl;

import static org.mockito.Mockito.mock;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MongoSensorFixStoreImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.EmptySensorFixStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixMapper;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogsensortracking.SensorFixMapperFactory;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.fixtracker.RaceLogFixTrackerManager;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sse.common.Timed;

public class RaceLogFixTrackerManagerTest {
    protected final MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
    protected final DeviceIdentifier device = new SmartphoneImeiIdentifierImpl("a");
    protected final DeviceIdentifier deviceTest = new SmartphoneImeiIdentifierImpl("b");
    protected RaceLog raceLog;
    protected RaceLog raceLog2;
    protected RegattaLog regattaLog;
    protected SensorFixStore store;
    protected final Competitor comp = DomainFactory.INSTANCE.getOrCreateCompetitor("comp", "comp", "c2", null, null, null,
            null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
    protected final Boat boat = DomainFactory.INSTANCE.getOrCreateBoat("boat", "boat", boatClass, "USA 123", null, /* storePersistently */ true);
    protected final Mark mark = DomainFactory.INSTANCE.getOrCreateMark("mark");
    protected final Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");

    protected final AbstractLogEventAuthor author = new LogEventAuthorImpl("author", 0);
    private DynamicTrackedRace trackedRace;

    @BeforeEach
    public void setUp() throws UnknownHostException, MongoException {
        raceLog = new RaceLogImpl("racelog");
        raceLog2 = new RaceLogImpl("racelog2");
        regattaLog = new RegattaLogImpl("regattalog");
        store = new MongoSensorFixStoreImpl(PersistenceFactory.INSTANCE.getDefaultMongoObjectFactory(),
                PersistenceFactory.INSTANCE.getDefaultDomainObjectFactory(), serviceFinderFactory, ReadConcern.MAJORITY,
                WriteConcern.MAJORITY, /* clientSession */ null, /* metadataCollectionClientSession */ null);
        Course course = new CourseImpl("course",
                Arrays.asList(new Waypoint[] { new WaypointImpl(mark), new WaypointImpl(mark2) }));
        Map<Competitor, Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(comp, boat);
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitorsAndBoats);
        DynamicTrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
        trackedRace = new DynamicTrackedRaceImpl(regatta, race, Collections.<Sideline> emptyList(),
                EmptyWindStore.INSTANCE, 0, 0, 0, /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
    }

    /**
     * Bug 4001: A {@link ConcurrentModificationException} was thrown when stopping tracking for a race with multiple
     * attached RaceLogs.
     */
    @Test
    public void testThatNoExceptionIsThrownWhenStoppingTrackingForRaceWithMultipleRaceLogs_bug4001() {
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.attachRaceLog(raceLog);
        trackedRace.attachRaceLog(raceLog2);
        RaceLogFixTrackerManager raceLogFixTrackerManager = new RaceLogFixTrackerManager(trackedRace,
                EmptySensorFixStore.INSTANCE, new SensorFixMapperFactory() {
                    @Override
                    public <FixT extends Timed, TrackT extends DynamicTrack<FixT>> SensorFixMapper<FixT, TrackT, Competitor> createCompetitorMapper(
                            Class<? extends RegattaLogDeviceMappingEvent<?>> eventType) {
                        throw new IllegalArgumentException("Unknown event type");
                    }
                }, /* removeOutliersFromCompetitorTracks */ true);
        raceLogFixTrackerManager.stop(/* preemptive */ false, /* willBeRemoved */ false);
    }

    /**
     * Bug 4001: A {@link ConcurrentModificationException} was thrown when stopping tracking for a race with multiple
     * attached RaceLogs.
     */
    @Test
    public void testThatNoExceptionIsThrownWhenStoppingTrackingWhenAddingSecondRaceLogWhileAlreadyTracking_bug4001() {
        trackedRace.attachRegattaLog(regattaLog);
        trackedRace.attachRaceLog(raceLog);
        RaceLogFixTrackerManager raceLogFixTrackerManager = new RaceLogFixTrackerManager(trackedRace,
                EmptySensorFixStore.INSTANCE, new SensorFixMapperFactory() {
                    @Override
                    public <FixT extends Timed, TrackT extends DynamicTrack<FixT>> SensorFixMapper<FixT, TrackT, Competitor> createCompetitorMapper(
                            Class<? extends RegattaLogDeviceMappingEvent<?>> eventType) {
                        throw new IllegalArgumentException("Unknown event type");
                    }
                }, /* removeOutliersFromCompetitorTracks */ true);
        trackedRace.attachRaceLog(raceLog2);
        raceLogFixTrackerManager.stop(/* preemptive */ false, /* willBeRemoved */ false);
    }
}
