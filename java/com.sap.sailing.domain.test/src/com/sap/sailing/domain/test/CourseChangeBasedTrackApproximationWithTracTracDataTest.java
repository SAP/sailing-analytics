package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.impl.CourseChangeBasedTrackApproximation;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public class CourseChangeBasedTrackApproximationWithTracTracDataTest extends OnlineTracTracBasedTest {
    private static final Logger logger = Logger.getLogger(CourseChangeBasedTrackApproximationWithTracTracDataTest.class.getName());

    private Iterable<Competitor> competitors;
    private CompetitorWithBoat sampleCompetitor;
    private DynamicGPSFixTrack<Competitor, GPSFixMoving> sampleTrack;

    public CourseChangeBasedTrackApproximationWithTracTracDataTest() throws MalformedURLException, URISyntaxException {
        super();
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS, ReceiverType.MARKPASSINGS });
        getTrackedRace().waitUntilNotLoading();
        assertFalse(Util.isEmpty(getTrackedRace().getRace().getCompetitors()));
        do {
            competitors = getTrackedRace().getRace().getCompetitors();
            // To pick a single competitor, e.g., for debugging, use the following line:
//            sampleCompetitor = (CompetitorWithBoat) Util.first(Util.filter(competitors, c->c.getName().equals("Feldmann")));
            // To pick a random competitor, use the following line:
            final Iterable<Competitor> validCompetitors = getValidCompetitors();
            sampleCompetitor = (CompetitorWithBoat) Util.get(validCompetitors, new Random().nextInt(Util.size(validCompetitors)));
            sampleTrack = getTrackedRace().getTrack(sampleCompetitor);
        } while (sampleTrack.isEmpty());
    }
    
    @Test
    public void testAllCompetitorsThatFinished() {
        for (final Competitor competitor : getValidCompetitors()) {
            sampleCompetitor = (CompetitorWithBoat) competitor;
            sampleTrack = getTrackedRace().getTrack(sampleCompetitor);
            logger.info("Testing competitor "+sampleCompetitor.getName());
            testNoDiffBetweenEarlyAndLateInitialization();
        }
    }

    private Iterable<Competitor> getValidCompetitors() {
        return Util.filter(competitors,
                c -> !c.getName().equals("Broise") && // Broise has major gaps in the track, leading to deviations, not relevant for production, so exclude them
                getTrackedRace().getMarkPassing(c, getTrackedRace().getRace().getCourse().getLastWaypoint()) != null);
    }
    
    /**
     * During the work on bug5959 (https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5959) we identified an issue
     * with early vs. late initialization of the approximation. When initializing the
     * {@link CourseChangeBasedTrackApproximation} objects before adding any fixes to the track, we received different
     * results than when initializing it after adding fixes and first trying to compute maneuvers. This goes against the
     * specification that the approximation should be independent of when it is initialized.<p>
     * 
     * To conduct the test, we take the fixes from a loaded competitor track, create a new {@link DynamicGPSFixMovingTrackImpl},
     * construct a {@link CourseChangeBasedTrackApproximation} based on this track (early initialization), then copy all fixes
     * from the loaded competitor track to the new test track (which adds these fixes to the approximation), then create a second
     * {@link CourseChangeBasedTrackApproximation} which will then add all fixes in one sweep. Then, we compare the results of
     * {@link CourseChangeBasedTrackApproximation#approximate(TimePoint, TimePoint)} for the duration of the entire track,
     * expecting them to be equal.
     */
    @Test
    public void testNoDiffBetweenEarlyAndLateInitialization() {
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> trackCopy = new DynamicGPSFixMovingTrackImpl<Competitor>(
                sampleCompetitor,
                /* millisecondsOverWhichToAverage */ boatClass.getApproximateManeuverDurationInMilliseconds());
        final CourseChangeBasedTrackApproximation earlyInitApproximation = new CourseChangeBasedTrackApproximation(
                trackCopy, sampleCompetitor.getBoat().getBoatClass(), /* logFixes for debugging */ false);
        final TimePoint from = sampleTrack.getFirstRawFix().getTimePoint();
        final TimePoint to = sampleTrack.getLastRawFix().getTimePoint();
        sampleTrack.lockForRead();
        try {
            for (final GPSFixMoving fix : sampleTrack.getRawFixes()) {
                trackCopy.add(fix);
            }
        } finally {
            sampleTrack.unlockAfterRead();
        }
        final CourseChangeBasedTrackApproximation lateInitApproximation = new CourseChangeBasedTrackApproximation(
                trackCopy, sampleCompetitor.getBoat().getBoatClass(), /* logFixes for debugging */ false);
        assertEquals(earlyInitApproximation.getNumberOfFixesAdded(), lateInitApproximation.getNumberOfFixesAdded(), "Number of fixes added to approximators differs");
        final Iterable<GPSFixMoving> earlyInitResult = earlyInitApproximation.approximate(from, to);
        final Iterable<GPSFixMoving> lateInitResult = lateInitApproximation.approximate(from, to);
        final Iterator<GPSFixMoving> earlyIter = earlyInitResult.iterator();
        final Iterator<GPSFixMoving> lateIter = lateInitResult.iterator();
        int i=0;
        while (earlyIter.hasNext() && lateIter.hasNext()) {
            final GPSFixMoving earlyFix = earlyIter.next();
            final GPSFixMoving lateFix = lateIter.next();
            if (earlyFix.getTimePoint().after(getTrackedRace().getStartOfRace()) && lateFix.getTimePoint().after(getTrackedRace().getStartOfRace())
                    && earlyFix.getTimePoint().before(getTrackedRace().getEndOfRace()) && lateFix.getTimePoint().before(getTrackedRace().getEndOfRace())) {
                assertEquals(earlyFix.getTimePoint(), lateFix.getTimePoint(), "Time points of approximation fixes differ at index "+i+" for competitor "+sampleCompetitor.getName());
            }
            i++;
        }
        assertEquals(Util.size(earlyInitResult), Util.size(lateInitResult), "Different numbers of approximation points for competitor "+sampleCompetitor.getName());
        assertEquals(Util.asSet(earlyInitResult), Util.asSet(lateInitResult));
    }
}
