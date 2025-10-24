package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * At the 505 Worlds 2015 Race R7 Arthur/Anderson (RSA 6266) have been tracked with a wild outlier
 * around 2015-04-02T15:05:07+0200.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class OutlierDetectionTest extends AbstractManeuverDetectionTestCase {
    public OutlierDetectionTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20150323_Worlds-505_Worlds_7.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/event_20150323_Worlds-505_Worlds_7.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
    }
    
    protected String getExpectedEventName() {
        return "505 Worlds 2015";
    }

    /**
     * Tests the 505 Race 7 for competitor "Arthur / Anderson" at a time where an outlier exists
     */
    @Test
    public void testArthurAndersonOutlierDetectionAndElimination() throws ParseException, NoWindException {
        Competitor competitor = getCompetitorByName("Arthur / Anderson");
        assertNotNull(competitor);
        Date fromDate = dateFormat.parse("04/02/2015-15:04:50");
        TimePoint from = new MillisecondsTimePoint(fromDate);
        Date toDate = dateFormat.parse("04/02/2015-15:06:20");
        TimePoint to = new MillisecondsTimePoint(toDate);
        assertNotNull(fromDate);
        assertNotNull(toDate);
        DynamicGPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(competitor);
        track.lockForRead();
        try {
            assertTrue(track.getMaximumSpeedOverGround(from, to).getB().getKnots() < 15.);
        } finally {
            track.unlockAfterRead();
        }
    }
}
