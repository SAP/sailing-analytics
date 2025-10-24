package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class SerializationWithOnlineRaceTest extends OnlineTracTracBasedTest {
    public SerializationWithOnlineRaceTest() throws MalformedURLException, URISyntaxException {
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
    
    private com.sap.sse.common.Util.Pair<Integer, Long> getSerializationSizeAndTime(Serializable s) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        long start = System.currentTimeMillis();
        new ObjectOutputStream(bos).writeObject(s);
        return new com.sap.sse.common.Util.Pair<Integer, Long>(bos.size(), System.currentTimeMillis()-start);
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
            com.sap.sse.common.Util.Pair<Integer, Long> sizeAndTime = getSerializationSizeAndTime(findelsTrack);
            System.out.println(sizeAndTime);
            assertTrue(sizeAndTime.getA() > 100000);
        } finally {
            findelsTrack.unlockAfterRead();
            cloneOfFindelsTrack.unlockAfterRead();
        }
    }
}
