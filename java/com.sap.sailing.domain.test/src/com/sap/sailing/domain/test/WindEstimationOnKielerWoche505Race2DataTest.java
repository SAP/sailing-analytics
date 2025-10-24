package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.TrackBasedEstimationWindTrackImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class WindEstimationOnKielerWoche505Race2DataTest extends OnlineTracTracBasedTest {

    public WindEstimationOnKielerWoche505Race2DataTest() throws MalformedURLException, URISyntaxException {
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        MillisecondsTimePoint timePointForFixes = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), timePointForFixes);
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, timePointForFixes, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(70))), new WindSourceImpl(WindSourceType.WEB));
    }
    
    /**
     * Checks that the {@link TrackBasedEstimationWindTrackImpl} data structure works. It does so by comparing the
     * results obtained from such a track with the results of immediately calling
     * {@link TrackedRace#getEstimatedWindDirection(TimePoint)}. The results
     * may not accurately equal each other because the track may consider more estimation values before and after
     * the time point for which the estimation is requested.
     */
    @Test
    public void testSimpleWindEstimationThroughEstimationTrack() throws NoWindException {
        // at this point in time, most boats are already going upwind again, and K�chlin, Neulen and Findel are tacking,
        // hence have a direction change.
        TimePoint middle = new MillisecondsTimePoint(1308839492322l);
        TrackBasedEstimationWindTrackImpl estimatedWindTrack = new TrackBasedEstimationWindTrackImpl(getTrackedRace(),
                WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND, WindSourceType.TRACK_BASED_ESTIMATION.getBaseConfidence(),
                /* delay for cache invalidation in milliseconds */ 0l);
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(middle);
        assertNotNull(estimatedWindDirection);
        Wind estimationBasedOnTrack = estimatedWindTrack.getAveragedWind(null, middle);
        assertEquals(estimatedWindDirection.getFrom().getDegrees(), estimationBasedOnTrack.getFrom().getDegrees(), 5.);
    }

    @Test
    public void testSetUp() {
        assertNotNull(getTrackedRace());
        final DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(getCompetitorByName("Dr.Plattner"));
        track.lockForRead();
        try {
            assertTrue(Util.size(track.getFixes()) > 1000);
        } finally {
            track.unlockAfterRead();
        }
    }

    @Test
    public void testSimpleWindEstimation() throws NoWindException {
        // at this point in time, a few boats are still going downwind, a few have passed the downwind
        // mark and are already going upwind again, and Lehmann is tacking, hence has a direction change.
        TimePoint middle = new MillisecondsTimePoint(1308839250105l);
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("Lehmann")).hasDirectionChange(middle, /* minimumDegreeDifference */ 9.));
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(middle);
        assertNotNull(estimatedWindDirection);
        assertEquals(243., estimatedWindDirection.getFrom().getDegrees(), 3.); // expect wind from 243 +/- 3 degrees
    }
    
    @Test
    public void testAnotherSimpleWindEstimation() throws NoWindException {
        // at this point in time, most boats are already going upwind again, and K�chlin, Neulen and Findel are tacking,
        // hence have a direction change.
        TimePoint middle = new MillisecondsTimePoint(1308839492322l);
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("K.chlin")).hasDirectionChange(middle, /* minimumDegreeDifference */ 15.));
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("Neulen")).hasDirectionChange(middle, /* minimumDegreeDifference */ 15.));
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("Findel")).hasDirectionChange(middle, /* minimumDegreeDifference */ 15.));
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(middle);
        assertNotNull(estimatedWindDirection);
        assertEquals(241., estimatedWindDirection.getFrom().getDegrees(), 4.); // expect wind from 241 +/- 3 degrees
    }
}
