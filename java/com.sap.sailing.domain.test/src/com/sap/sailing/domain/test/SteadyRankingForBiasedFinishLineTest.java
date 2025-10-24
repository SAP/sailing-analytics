package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * See bug 5004
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class SteadyRankingForBiasedFinishLineTest extends OnlineTracTracBasedTest {
    final static MillisecondsTimePoint TIME_14_30_00 = new MillisecondsTimePoint(1555849800000l);
    final static MillisecondsTimePoint TIME_14_30_12 = new MillisecondsTimePoint(1555849812000l);

    public SteadyRankingForBiasedFinishLineTest() throws MalformedURLException, URISyntaxException {
    }
    
    @Override
    protected String getExpectedEventName() {
        return "Sailing World Cup Genoa 2019";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/0fb18f50-45d7-0137-24dd-021d89ada30e.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/0fb18f50-45d7-0137-24dd-021d89ada30e.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE,
                        ReceiverType.RACESTARTFINISH });
        getTrackedRace().recordWind(new WindImpl(/* position */ new DegreePosition(44.37670797575265, 8.925960855558515), TIME_14_30_00,
                new KnotSpeedWithBearingImpl(7.5, new DegreeBearingImpl(246).reverse())), new WindSourceImpl(WindSourceType.WEB));
    }
    
    @Test
    public void testVadnaiLead() {
        Competitor vadnai = getCompetitorByName("Benjamin Vadnai");
        Competitor lewis = getCompetitorByName("Andrew Lewis");
        Competitor lynch = getCompetitorByName("Finn Lynch");
        Competitor blanco = getCompetitorByName("Joaquín Blanco");
        assertEquals(1, getTrackedRace().getRank(vadnai, TIME_14_30_00));
        assertEquals(2, getTrackedRace().getRank(lewis, TIME_14_30_00));
        assertEquals(3, getTrackedRace().getRank(lynch, TIME_14_30_00));
        assertEquals(4, getTrackedRace().getRank(blanco, TIME_14_30_00));
        assertEquals(1, getTrackedRace().getRank(vadnai, TIME_14_30_12));
        assertEquals(2, getTrackedRace().getRank(lewis, TIME_14_30_12));
        assertEquals(3, getTrackedRace().getRank(lynch, TIME_14_30_12));
        assertEquals(4, getTrackedRace().getRank(blanco, TIME_14_30_12));
    }

}
