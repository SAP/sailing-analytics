package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TimeInLegProgressesTest extends OnlineTracTracBasedTest {
    public TimeInLegProgressesTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), new MillisecondsTimePoint(
                new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(65))), new WindSourceImpl(WindSourceType.WEB));
    }
    
    @Test
    public void testDifferentTimesSpentInLegAtDifferentTimes() throws ClassNotFoundException, IOException {
        final Competitor findel = getCompetitorByName("Findel");
        TrackedLegOfCompetitor findelsSecondLeg = getTrackedRace().getTrackedLeg(getTrackedRace().getRace().getCourse().getLegs().get(1)).getTrackedLeg(findel);
        TimePoint findelStartedHisSecondLegAt = findelsSecondLeg.getStartTime();
        assertEquals(null, findelsSecondLeg.getTime(findelStartedHisSecondLegAt.minus(1)));
        assertEquals(0.0, findelsSecondLeg.getTime(findelStartedHisSecondLegAt).asMillis(), 0.00001);
        assertEquals(10000.0, findelsSecondLeg.getTime(findelStartedHisSecondLegAt.plus(10000)).asMillis(), 0.00001);
    }
}
