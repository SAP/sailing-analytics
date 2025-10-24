package com.sap.sailing.domain.tracking.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
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
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.TrackedRace;

public class TrackedRegattaTest {

    private final Mark mark = DomainFactory.INSTANCE.getOrCreateMark("mark");
    private final Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");

    private Course course = new CourseImpl("course",
            Arrays.asList(new Waypoint[] { new WaypointImpl(mark), new WaypointImpl(mark2) }));
    private DynamicTrackedRegatta regatta;
    
    @BeforeEach
    public void setUp() {
        regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ false, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));
    }
    
    @Test
    public void testBug4429() throws Exception {
        assertThrows(TimeoutException.class, ()->{
            final Phaser addPhaser = new Phaser(2);
            final CyclicBarrier removeBarrier = new CyclicBarrier(2);
            
            regatta.addRaceListener(new RaceListener() {
                @Override
                public void raceRemoved(TrackedRace trackedRace) {
                    try {
                        removeBarrier.await(1, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                
                @Override
                public void raceAdded(TrackedRace trackedRace) {
                    try {
                        addPhaser.arriveAndAwaitAdvance();
                        addPhaser.arriveAndAwaitAdvance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }, Optional.empty(), /* synchronous */ false);
            
            DynamicTrackedRace race1 = createRace("R1");
            Thread thread1 = new Thread(() -> {
                regatta.addTrackedRace(race1, Optional.empty());
            });
            thread1.start();
            // This ensures, that the add event is being processed but is not finished because
            // this unblocks the first arriveAndAwaitAdvance() in raceAdded, but not the second.
            // This way, the raceRemoved(...) call is expected to not be started because it has
            // to wait for the raceAdded(...) call to have finished.
            addPhaser.arriveAndAwaitAdvance();
            
            Thread thread2 = new Thread(() -> {
                regatta.removeTrackedRace(race1, Optional.empty());
            });
            thread2.start();
            // If the implementation ensures that the events are fired in order,
            // the removeBarrier will run into a TimeoutException because the addBarrier
            // is not solved and while the first event is processed the second one
            // should not be started to be processed.
            try {
                removeBarrier.await(10, TimeUnit.MILLISECONDS);
                // if this line is reached, the order of events is not correctly ensured
                Assertions.fail();
            } finally {
                addPhaser.forceTermination();
                try {
                    // solved the barrier if one hangs at this point
                    // a TimeoutException can occur but we do not care about it
                    removeBarrier.await(10, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                }
                thread1.join(1000);
                thread2.join(1000);
            }
        });
    }
    
    private DynamicTrackedRace createRace(String name) {
        RaceDefinition race1 = new RaceDefinitionImpl(name, course, boatClass, Collections.emptyMap());
        return new DynamicTrackedRaceImpl(regatta, race1, Collections.<Sideline> emptyList(),
                EmptyWindStore.INSTANCE, 0, 0, 0, /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
    }
}
