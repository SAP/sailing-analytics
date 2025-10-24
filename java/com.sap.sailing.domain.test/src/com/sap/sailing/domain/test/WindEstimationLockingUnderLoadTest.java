package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.TrackBasedEstimationWindTrackImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * See the issues documented in bug 1923 (http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=1923).
 * This test mocks {@link TrackedRace} to the degree necessary and fires more or less random "measured"
 * wind fixes against the {@link TrackBasedEstimationWindTrackImpl} while concurrently reading massively
 * from it. With this, the test is supposed to reproduce conditions as observed during events, leading
 * up to bug 1923. This can then be the basis for a fix.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class WindEstimationLockingUnderLoadTest {
    private static final Logger logger = Logger.getLogger(WindEstimationLockingUnderLoadTest.class.getName());
    
    private static final int MIN_WIND_SPEED_IN_KNOTS = 5;

    private static final int MAX_WIND_SPEED_IN_KNOTS = 20;

    private TrackedRace mockedTrackedRace;
    
    private WindSource realWindSource;
    
    private TrackBasedEstimationWindTrackImpl estimationTrack;
    
    private WindTrack measuredTrack;
    
    @BeforeEach
    public void setUp() {
        realWindSource = new WindSourceWithAdditionalID(WindSourceType.EXPEDITION, "1");
        mockedTrackedRace = mockTrackedRace();
        estimationTrack = new TrackBasedEstimationWindTrackImpl(mockedTrackedRace, WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND, 0.5);
    }
    
    @AfterEach
    public void tearDown() {
        // clean up all Mockito stubbing leaks, particularly the InvocationImpl objects attached to any ThreadLocal;
        // see also bug 1923, comment #9.
        Mockito.reset();
    }

    private TrackedRace mockTrackedRace() {
        final BoatClass boatClass = new BoatClassImpl("Some Handicap Boat Class", /* typicallyStartsUpwind */ true);
        final Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", /* courseArea */ null,
                /* controlTrackingFromStartAndFinishTimes */ false, /* autoRestartTrackingUponCompetitorSetChange */ false,
                OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
        final TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        final List<Waypoint> waypoints = Collections.emptyList();
        Course course = new CourseImpl("Test Course", waypoints);
        RaceDefinition mockedRaceDefinition = new RaceDefinitionImpl("Test Race", course, boatClass, Collections.emptyMap());
        DynamicTrackedRace result = new DynamicTrackedRaceImpl(trackedRegatta, mockedRaceDefinition,
                /* sidelines */ Collections.emptySet(), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 10000,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 15000,
                /* useInternalMarkPassingAlgorithm */ false, regatta.getRankingMetricConstructor(), /* raceLogResolver */ null,
                /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public WindWithConfidence<TimePoint> getEstimatedWindDirectionWithConfidence(TimePoint timePoint) {
                        return randomWindOrNull();
                    }
        };
        result.setStartTimeReceived(TimePoint.now().minus(Duration.ONE_MINUTE.times(5)));
        result.setStatus(new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, /* loadingProgress */ 1.0));
        measuredTrack = result.getOrCreateWindTrack(realWindSource);
        assertEquals(30000l, result.getMillisecondsOverWhichToAverageWind());
        return result;
    }
    
    private WindWithConfidence<TimePoint> randomWindOrNull() {
        final WindWithConfidence<TimePoint> result;
        double speedInKnots = MAX_WIND_SPEED_IN_KNOTS*Math.random();
        if (speedInKnots < MIN_WIND_SPEED_IN_KNOTS) {
            result = null;
        } else {
            double directionInDegrees = 360*Math.random();
            double confidence = Math.random();
            final TimePoint now = MillisecondsTimePoint.now();
            result = new WindWithConfidenceImpl<TimePoint>(new WindImpl(new DegreePosition(49, 8), now, new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(directionInDegrees))),
                    confidence, now, /* useSpeed */ true);
        }
        return result;
    }

    @Test
    public void testSimpleWindEstimation() {
        // We can expect MIN_WIND_SPEED_IN_KNOTS/MAX_WIND_SPEED_IN_KNOTS results to be null, but as there is a significant
        // difference between the two, with a sufficient amount of random samples there has to be at least one non-null
        // result
        int notNullCount = 0;
        for (int i=0; i<1000; i++) {
            Wind estimatedWind = estimationTrack.getAveragedWind(/* position */ null, MillisecondsTimePoint.now().minus(10000*i));
            if (estimatedWind != null) {
                notNullCount++;
            }
        }
        assertTrue(notNullCount > 0);
    }
    
    @Test
    public void testSendingNewWindFixToEstimationTrack() {
        final WindImpl wind = new WindImpl(new DegreePosition(49, 8), MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(/* speedInKnots */12., new DegreeBearingImpl(47)));
        addMeasuredWindFix(wind);
        // no verdict, really; just expecting that there is no exception raised
    }
    
    @Test
    public void testAddingManyWindFixesWhileReadingLikeCrazy() throws InterruptedException {
        Thread inserter = new Thread("Inserter thread in "+getClass().getName()+".testAddingManyWindFixesWhileReadingLikeCrazy") {
            @Override
            public void run() {
                TimePoint now = MillisecondsTimePoint.now();
                for (int i = 0; i < 10000; i++) {
                    final WindImpl wind = new WindImpl(new DegreePosition(49, 8), now, new KnotSpeedWithBearingImpl(
                            /* speedInKnots */12., new DegreeBearingImpl(47)));
                    addMeasuredWindFix(wind);
                    now = now.plus(10);
                }
                logger.info("Inserter thread done");
            }
        };
        Runnable readerRunnable = new Runnable() {
            @Override
            public void run() {
                TimePoint now = MillisecondsTimePoint.now();
                for (int i=0; i<10000; i++) {
                    estimationTrack.getAveragedWind(/* position */ null, now);
                    now = now.plus(10);   // the cache quantizes to a full second; so not all time points will automatically fetch uncached values
                }
                logger.info(Thread.currentThread().getName()+" done");
            }
        };
        inserter.start();
        List<Thread> readers = new ArrayList<Thread>();
        for (int i = 0; i < 4; i++) {
            Thread reader = new Thread(readerRunnable, "Reader thread "+i+" in " + getClass().getName()
                    + ".testAddingManyWindFixesWhileReadingLikeCrazy");
            reader.start();
            readers.add(reader);
        }
        inserter.join();
        for (Thread reader : readers) {
            reader.join();
        }
    }

    /**
     * Adds the wind fix to the {@link #measuredTrack} and explicitly notifies {@link #estimationTrack} which is
     * not a listener on the mocked tracked race or the wind track and therefore otherwise wouldn't receive the fix
     */
    private void addMeasuredWindFix(final WindImpl wind) {
        measuredTrack.add(wind);
        estimationTrack.windDataReceived(wind, realWindSource);
    }
}
