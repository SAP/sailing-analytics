package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.TrackBasedEstimationWindTrackImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;

public class WindEstimationOnKielerWoche505Race2DataTest extends OnlineTracTracBasedTest {

    public WindEstimationOnKielerWoche505Race2DataTest() throws MalformedURLException, URISyntaxException {
    }
    
    @Before
    public void setUp() throws MalformedURLException, IOException, InterruptedException, URISyntaxException {
        super.setUp();
        super.setUp("event_20110609_KielerWoch",
        /* raceId */"357c700a-9d9a-11e0-85be-406186cbf87c", new ReceiverType[] { ReceiverType.MARKPASSINGS,
                ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        MillisecondsTimePoint timePointForFixes = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), timePointForFixes);
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, timePointForFixes, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(70))), new WindSourceImpl(WindSourceType.WEB));
    }
    
    /**
     * Checks that the {@link TrackBasedEstimationWindTrackImpl} data structure works. It does so by comparing the
     * results obtained from such a track with the results of immediately calling
     * {@link TrackedRace#getEstimatedWindDirection(com.sap.sailing.domain.base.Position, TimePoint)}. The results
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
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, middle);
        assertNotNull(estimatedWindDirection);
        Wind estimationBasedOnTrack = estimatedWindTrack.getAveragedWind(null, middle);
        assertEquals(estimatedWindDirection.getFrom().getDegrees(), estimationBasedOnTrack.getFrom().getDegrees(), 5.);
    }

    @Test
    public void testSetUp() {
        assertNotNull(getTrackedRace());
        assertTrue(Util.size(getTrackedRace().getTrack(getCompetitorByName("Dr.Plattner")).getFixes()) > 1000);
    }

    @Test
    public void testSimpleWindEstimation() throws NoWindException {
        // at this point in time, a few boats are still going downwind, a few have passed the downwind
        // mark and are already going upwind again, and Lehmann is tacking, hence has a direction change.
        TimePoint middle = new MillisecondsTimePoint(1308839250105l);
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("Lehmann")).hasDirectionChange(middle, /* minimumDegreeDifference */ 9.));
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, middle);
        assertNotNull(estimatedWindDirection);
        assertEquals(241., estimatedWindDirection.getFrom().getDegrees(), 3.); // expect wind from 241 +/- 3 degrees
    }
    
    @Test
    public void testAnotherSimpleWindEstimation() throws NoWindException {
        // at this point in time, most boats are already going upwind again, and K�chlin, Neulen and Findel are tacking,
        // hence have a direction change.
        TimePoint middle = new MillisecondsTimePoint(1308839492322l);
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("K.chlin")).hasDirectionChange(middle, /* minimumDegreeDifference */ 15.));
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("Neulen")).hasDirectionChange(middle, /* minimumDegreeDifference */ 15.));
        assertTrue(getTrackedRace().getTrack(getCompetitorByName("Findel")).hasDirectionChange(middle, /* minimumDegreeDifference */ 15.));
        Wind estimatedWindDirection = getTrackedRace().getEstimatedWindDirection(/* position */ null, middle);
        assertNotNull(estimatedWindDirection);
        assertEquals(237., estimatedWindDirection.getFrom().getDegrees(), 4.); // expect wind from 241 +/- 3 degrees
    }
}
