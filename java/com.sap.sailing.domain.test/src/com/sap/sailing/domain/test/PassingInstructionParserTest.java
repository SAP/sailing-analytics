package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PassingInstructionParserTest extends OnlineTracTracBasedTest {

    public PassingInstructionParserTest() throws URISyntaxException, MalformedURLException {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20131112_ESSFlorian-Race_1.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/event_20131112_ESSFlorian-Race_1.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.RACECOURSE });
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(10))), new WindSourceImpl(WindSourceType.WEB));
    }

    @Override
    protected String getExpectedEventName() {
        return "ESS Florianopolis 2013";
    }

    @Test
    public void test() {
        int i = 0;
        for (Waypoint w : getRace().getCourse().getWaypoints()) {
            if (w.getPassingInstructions() == PassingInstruction.Starboard) {
                i++;
            }
        }
        assertEquals(2, i);
    }
}
