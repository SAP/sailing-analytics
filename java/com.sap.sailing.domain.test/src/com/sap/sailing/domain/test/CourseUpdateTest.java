package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseListener;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl;
import com.sap.sailing.domain.tractracadapter.impl.RaceCourseReceiver;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.map.IPositionedItem;
import com.tractrac.model.lib.api.metadata.IMetadata;
import com.tractrac.model.lib.api.route.IControlRoute;
import com.tractrac.subscription.lib.api.event.IConnectionStatusListener;
import com.tractrac.subscription.lib.api.event.ILiveDataEvent;
import com.tractrac.subscription.lib.api.event.IStoredDataEvent;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

public class CourseUpdateTest extends AbstractTracTracLiveTest {
    private RaceDefinition race;
    private Course course;
    private Regatta domainRegatta;
    private DynamicTrackedRegatta trackedRegatta;
    private CompletableFuture<IControlRoute> routeDataFuture;
    private DomainFactory domainFactory;

    public CourseUpdateTest() throws URISyntaxException, MalformedURLException {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        routeDataFuture = new CompletableFuture<>();
        domainFactory = new DomainFactoryImpl(new com.sap.sailing.domain.base.impl.DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory.TEST_RACE_LOG_RESOLVER));
        domainRegatta = domainFactory.getOrCreateDefaultRegatta(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                getTracTracRace(), /* trackedRegattaRegistry */ null);
        trackedRegatta = new DynamicTrackedRegattaImpl(domainRegatta);
        IRace tractracRace = getTracTracRace();
        ArrayList<Receiver> receivers = new ArrayList<Receiver>();
        receivers.add(new RaceCourseReceiver(domainFactory, trackedRegatta, getTracTracEvent(), tractracRace,
                EmptyWindStore.INSTANCE, new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                        setTrackedRace((DynamicTrackedRaceImpl) trackedRace);
                    }
                },
                /* delayToLiveInMillis */0l, /* millisecondsOverWhichToAverageWind */30000, /* simulator */null, /* courseDesignUpdateURI */
                null, /* tracTracUsername */null, /* tracTracPassword */null, getEventSubscriber(), getRaceSubscriber(),
                /*ignoreTracTracMarkPassings*/false, mock(RaceLogAndTrackedRaceResolver.class), mock(LeaderboardGroupResolver.class), RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                new DefaultRaceTrackingHandler(), /* markPassingRaceFingerprintRegistry */ null) {
            @Override
            protected void handleEvent(Triple<IControlRoute, Long, Void> event) {
                super.handleEvent(event);
                if (!event.getA().getControls().isEmpty()) {
                    routeDataFuture.complete(event.getA());
                }
            }
        });
        final Semaphore loadingStatusSemaphore = new Semaphore(0);
        getRaceSubscriber().subscribeConnectionStatus(new IConnectionStatusListener() {
            @Override
            public void gotStoredDataEvent(IStoredDataEvent storedDataEvent) {
            }

            @Override
            public void gotLiveDataEvent(ILiveDataEvent liveDataEvent) {
            }

            @Override
            public void stopped(Object subscribedObject) {
                loadingStatusSemaphore.release();
            }
        });
        // now we expect that there is no 
        assertNull(domainFactory.getExistingRaceDefinitionForRace(tractracRace.getId()));
        addListenersForStoredDataAndStartController(receivers);
        loadingStatusSemaphore.acquire();
        final Semaphore semaphore = new Semaphore(0);
        for (final Receiver receiver : receivers) {
            receiver.callBackWhenLoadingQueueIsDone(r->semaphore.release());
            addReceiverToStopDuringTearDown(receiver);
        }
        semaphore.acquire(receivers.size()); // continue only after all receivers have finished loading their queue
        race = domainFactory.getAndWaitForRaceDefinition(tractracRace.getId());
        course = race.getCourse();
        assertNotNull(course);
        assertEquals(3, Util.size(course.getWaypoints()));
        // make sure leg is initialized correctly in CourseImpl
        assertEquals("start/finish", course.getLegs().get(0).getFrom().getName());
        assertEquals("top", course.getLegs().get(1).getFrom().getName());
    }

    /**
     * Asserts that the race course's legs have corresponding {@link TrackedLeg}s and the {@link TrackedLeg}s have
     * {@link TrackedLegOfCompetitor} for each of the race's competitors.
     */
    @Test
    public void testSuccessfulSetup() throws InterruptedException {
        testLegStructure(2);
    }

    private void testLegStructure(int minimalNumberOfLegsExpected) throws InterruptedException {
        waitForRouteData();
        course.lockForRead();
        try {
            assertTrue(course.getLegs().size() >= minimalNumberOfLegsExpected);
            TrackedRace trackedRace = trackedRegatta.getTrackedRace(race);
            assertEquals(course.getLegs().size(), Util.size(trackedRace.getTrackedLegs()));
            Iterator<Leg> legIter = course.getLegs().iterator();
            for (TrackedLeg trackedLeg : trackedRace.getTrackedLegs()) {
                assertTrue(legIter.hasNext());
                Leg leg = legIter.next();
                assertSame(leg, trackedLeg.getLeg());
                for (Competitor competitor : race.getCompetitors()) {
                    TrackedLegOfCompetitor tloc = trackedLeg.getTrackedLeg(competitor);
                    assertNotNull(tloc);
                    assertSame(competitor, tloc.getCompetitor());
                    assertSame(leg, tloc.getLeg());
                }
            }
        } finally {
            course.unlockAfterRead();
        }
    }
    
    @Test
    public void testWaypointListDiff() {
        Waypoint wp1 = new WaypointImpl(new MarkImpl("b1"));
        Waypoint wp2 = new WaypointImpl(new MarkImpl("b2"));
        Waypoint wp3 = new WaypointImpl(new MarkImpl("b3"));
        Waypoint wp4 = new WaypointImpl(new MarkImpl("b4"));
        List<Waypoint> waypoints = new ArrayList<Waypoint>(4);
        waypoints.add(wp1);
        waypoints.add(wp2);
        waypoints.add(wp3);
        waypoints.add(wp4);
        List<Waypoint> changedWaypoints = new ArrayList<Waypoint>(3);
        changedWaypoints.add(wp1);
        changedWaypoints.add(wp3);
        changedWaypoints.add(wp4);

        Patch<Waypoint> patch = DiffUtils.diff(waypoints, changedWaypoints);
        assertEquals(1, patch.getDeltas().size());
        Delta<Waypoint> firstDelta = patch.getDeltas().iterator().next();
        assertEquals(Delta.TYPE.DELETE, firstDelta.getType());
        Chunk<Waypoint> original = firstDelta.getOriginal();
        assertEquals(1, original.getPosition());
        List<Waypoint> deletedWaypoints = original.getLines();
        assertEquals(1, deletedWaypoints.size());
        assertEquals(wp2, deletedWaypoints.iterator().next());
    }

    /**
     * A test failing before bug 2858 was fixed: updating only the {@link PassingInstructions} of a {@link Waypoint#getPassingInstructions() waypoint}
     * would have left the waypoint in place, not updating the course properly.
     */
    @Test
    public void testWaypointListDiffWithDifferentPassingInstructionsOnly() throws PatchFailedException {
        Waypoint wp1 = new WaypointImpl(new MarkImpl("b1"), PassingInstruction.Port);
        Waypoint wp2 = new WaypointImpl(new MarkImpl("b2"), PassingInstruction.Port);
        Waypoint wp3 = new WaypointImpl(new MarkImpl("b3"), PassingInstruction.Port);
        Waypoint wp4 = new WaypointImpl(new MarkImpl("b4"), PassingInstruction.Port);
        List<Waypoint> waypoints = new ArrayList<Waypoint>(4);
        waypoints.add(wp1);
        waypoints.add(wp2);
        waypoints.add(wp3);
        waypoints.add(wp4);
        Course course = new CourseImpl("Test Course", waypoints);
        List<Pair<ControlPoint, PassingInstruction>> changedControlPoints = new ArrayList<>();
        changedControlPoints.add(new Pair<>(wp1.getControlPoint(), PassingInstruction.Starboard));
        changedControlPoints.add(new Pair<>(wp2.getControlPoint(), wp2.getPassingInstructions()));
        changedControlPoints.add(new Pair<>(wp3.getControlPoint(), wp3.getPassingInstructions()));
        changedControlPoints.add(new Pair<>(wp4.getControlPoint(), wp4.getPassingInstructions()));
        course.update(changedControlPoints, new HashMap<>(), course.getOriginatingCourseTemplateIdOrNull(),
                com.sap.sailing.domain.base.DomainFactory.INSTANCE);
        
        assertNotSame(wp1, Util.get(course.getWaypoints(), 0));
        assertSame(wp2, Util.get(course.getWaypoints(), 1));
        assertSame(wp3, Util.get(course.getWaypoints(), 2));
        assertSame(wp4, Util.get(course.getWaypoints(), 3));
    }

    @Test
    public void testLastWaypointRemoved() throws PatchFailedException, InterruptedException {
        final boolean[] result = new boolean[1];
        final IControlRoute routeData = waitForRouteData();
        final List<IMapItem> controlPoints = new ArrayList<>(routeData.getControls());
        final IMapItem removedControlPoint = controlPoints.remove(controlPoints.size()-1);
        course.addCourseListener(new CourseListener() {
            @Override
            public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
                System.out.println("waypointAdded " + zeroBasedIndex + " / " + waypointThatGotAdded);
            }

            @Override
            public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
                System.out.println("waypointRemoved " + zeroBasedIndex + " / " + waypointThatGotRemoved);
                ControlPoint cp = domainFactory.getOrCreateControlPoint(removedControlPoint);
                result[0] = zeroBasedIndex == controlPoints.size() && waypointThatGotRemoved.getControlPoint() == cp;
            }
        });
        domainFactory.updateCourseWaypoints(course, getTracTracControlPointsWithPassingInstructions(controlPoints));
        assertTrue(result[0]);
        testLegStructure(1);
    }

    @Test
    public void testLastButOneWaypointRemoved() throws PatchFailedException, InterruptedException {
        final boolean[] result = new boolean[1];
        final IControlRoute routeData = waitForRouteData();
        final List<IMapItem> controlPoints = new ArrayList<>(routeData.getControls());
        final IMapItem removedControlPoint = controlPoints.remove(1);
        course.addCourseListener(new CourseListener() {
            @Override
            public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
                System.out.println("waypointAdded " + zeroBasedIndex + " / " + waypointThatGotAdded);
            }

            @Override
            public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
                System.out.println("waypointRemoved " + zeroBasedIndex + " / " + waypointThatGotRemoved);
                ControlPoint cp = domainFactory.getOrCreateControlPoint(removedControlPoint);
                result[0] = zeroBasedIndex == 1 && waypointThatGotRemoved.getControlPoint() == cp;
            }
        });
        domainFactory.updateCourseWaypoints(course, getTracTracControlPointsWithPassingInstructions(controlPoints));
        assertTrue(result[0]);
        testLegStructure(1);
    }

    private IControlRoute waitForRouteData() {
        try {
            return routeDataFuture.get(1, TimeUnit.MINUTES);
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testWaypointAddedAtEnd() throws PatchFailedException, InterruptedException {
        final boolean[] result = new boolean[1];
        final IMapItem cp1 = createMockedControlPoint("CP1", 1, UUID.randomUUID());
        final IControlRoute routeData = waitForRouteData();
        final List<IMapItem> controlPoints = new ArrayList<>(routeData.getControls());
        controlPoints.add(cp1);
        course.addCourseListener(new CourseListener() {
            @Override
            public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
                System.out.println("waypointAdded " + zeroBasedIndex + " / " + waypointThatGotAdded);
                ControlPoint cp = domainFactory.getOrCreateControlPoint(cp1);
                result[0] = zeroBasedIndex == controlPoints.size() - 1 && waypointThatGotAdded.getControlPoint() == cp;
            }

            @Override
            public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
                System.out.println("waypointRemoved " + zeroBasedIndex + " / " + waypointThatGotRemoved);
            }
        });
        ((CourseImpl) course).lockForWrite(); // without the lock it's possible that another race course update removes the additional waypoint again
        try {
            domainFactory.updateCourseWaypoints(course, getTracTracControlPointsWithPassingInstructions(controlPoints));
            assertTrue(result[0]);
            testLegStructure(3);
        } finally {
            ((CourseImpl) course).unlockAfterWrite();
        }
    }

    private IMapItem createMockedControlPoint(String name, int numberOfMarks, UUID id) {
        final IMapItem cp1 = Mockito.mock(IMapItem.class);
        Mockito.when(cp1.getName()).thenReturn(name);
        Mockito.when(cp1.getSize()).thenReturn(numberOfMarks);
        Mockito.when(cp1.getId()).thenReturn(id);
        final List<IPositionedItem> markMocks = new ArrayList<>();
        for (int i=0; i<numberOfMarks; i++) {
            final IPositionedItem markMock = Mockito.mock(IPositionedItem.class);
            final UUID markId = UUID.randomUUID();
            Mockito.when(markMock.getId()).thenReturn(markId);
            Mockito.when(markMock.getName()).thenReturn(name+" ("+i+")");
            final IMetadata markMockMetadata = Mockito.mock(IMetadata.class);
            Mockito.when(markMockMetadata.getText()).thenReturn("");
            Mockito.when(markMock.getMetadata()).thenReturn(markMockMetadata);
            markMocks.add(markMock);
        }
        Mockito.when(cp1.getPositionedItems()).thenReturn(markMocks);
        return cp1;
    }
    
    @Test
    public void testTrackedRacesTrackedLegsUpdatedProperly() throws InterruptedException, PatchFailedException {
        final boolean[] result = new boolean[1];
        final IMapItem cp1 = createMockedControlPoint("CP1", 1, UUID.randomUUID());
        final IControlRoute routeData = waitForRouteData();
        final List<IMapItem> controlPoints = new ArrayList<>(routeData.getControls());
        controlPoints.add(cp1);
        course.addCourseListener(new CourseListener() {
            @Override
            public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
                System.out.println("waypointAdded " + zeroBasedIndex + " / " + waypointThatGotAdded);
                ControlPoint cp = domainFactory.getOrCreateControlPoint(cp1);
                result[0] = zeroBasedIndex == controlPoints.size() - 1 && waypointThatGotAdded.getControlPoint() == cp;
            }

            @Override
            public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
                System.out.println("waypointRemoved " + zeroBasedIndex + " / " + waypointThatGotRemoved);
            }
        });
        domainFactory.updateCourseWaypoints(course, getTracTracControlPointsWithPassingInstructions(controlPoints));
        assertTrue(result[0]);
        testLegStructure(3);
    }
    
    @AfterEach
    public void tearDown() throws MalformedURLException, IOException, InterruptedException {
        super.tearDown();
        routeDataFuture = null;
        domainFactory.removeRace(getTracTracRace().getEvent(), getTracTracRace(),
                getTrackedRace().getTrackedRegatta().getRegatta(), /* trackedRegattaRegistry */ null);
    }
}
