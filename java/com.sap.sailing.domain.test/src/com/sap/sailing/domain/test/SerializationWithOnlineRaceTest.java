package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;

public class SerializationWithOnlineRaceTest extends OnlineTracTracBasedTest {
    public SerializationWithOnlineRaceTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Before
    public void setUp() throws URISyntaxException, IOException, InterruptedException, ParseException {
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
    
    private Pair<Integer, Long> getSerializationSizeAndTime(Serializable s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        long start = System.currentTimeMillis();
        new ObjectOutputStream(bos).writeObject(s);
        return new Pair<Integer, Long>(bos.size(), System.currentTimeMillis()-start);
    }
    
    @Test
    public void testSerializingGPSTrack() throws ClassNotFoundException, IOException {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> findelsTrack = getTrackedRace().getTrack(getCompetitorByName("Findel"));
        DynamicGPSFixTrack<Competitor, GPSFixMoving> cloneOfFindelsTrack = AbstractSerializationTest
                .cloneBySerialization(findelsTrack, DomainFactory.INSTANCE);
        findelsTrack.lockForRead();
        cloneOfFindelsTrack.lockForRead();
        try {
            assertEquals(Util.size(findelsTrack.getFixes()), Util.size(cloneOfFindelsTrack.getFixes()));
            Pair<Integer, Long> sizeAndTime = getSerializationSizeAndTime(findelsTrack);
            System.out.println(sizeAndTime);
            assertTrue(sizeAndTime.getA() > 100000);
        } finally {
            findelsTrack.unlockAfterRead();
            cloneOfFindelsTrack.unlockAfterRead();
        }
    }
}
