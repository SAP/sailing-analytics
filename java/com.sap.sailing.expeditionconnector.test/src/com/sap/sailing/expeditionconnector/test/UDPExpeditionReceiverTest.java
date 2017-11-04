package com.sap.sailing.expeditionconnector.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.test.mock.MockedTrackedRace;
import com.sap.sailing.expeditionconnector.ExpeditionListener;
import com.sap.sailing.expeditionconnector.ExpeditionMessage;
import com.sap.sailing.expeditionconnector.ExpeditionTrackerFactory;
import com.sap.sailing.expeditionconnector.ExpeditionWindTracker;
import com.sap.sailing.expeditionconnector.UDPExpeditionReceiver;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class UDPExpeditionReceiverTest {
    @Rule public Timeout TestTimeout = new Timeout(60 * 1000);
    
    private String[] validLines;
    private String[] someValidWithFourInvalidLines;
    final List<ExpeditionMessage> messages = new ArrayList<ExpeditionMessage>();
    final int PORT = 9876;
    private DatagramPacket packet;
    private DatagramSocket socket;
    private byte[] buf;
    private UDPExpeditionReceiver receiver;
    private ExpeditionListener listener;
    private Thread receiverThread;

    @Before
    public void setUp() throws UnknownHostException, SocketException, InterruptedException {
        validLines = new String[] {
                "#0,1,7.700,2,-39.0,3,23.00,9,319.0,12,1.17,146,40348.390035*37",
                "#0,4,-54.9,5,17.69,6,263.1,9,318.0*0D",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,1,7.700,2,-39.0,3,24.30,9,318.0,12,1.07,50,326.3,146,40348.390046*18",
                "#0,4,-53.8,5,18.95,6,266.2,9,320.0*0A",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,1,7.700,2,-36.0,3,25.10,4,-49.5,5,19.41,6,271.5,9,321.0,12,1.07,50,327.3,146,40348.390058*10",
                "#0,9,321.0*04",
                "#5,2,-163.2,3,0.00,13,305.8,39,2.0,48,39.500717,49,2.747750*X19"
        };

        someValidWithFourInvalidLines = new String[] {
                "#0,1,7.700,2,-39.0,3,23.00,9,319.0,12,1.17,146,40348.390035*37",
                "#0,4,-54.9,5,17.69,6,263.1,9,318.0*0D",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0F", // invalid
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,9,318.0*0E",
                "#0,1,7.700,2,-39.0,3,24.30,9,318.0,12,1.07,50,326.3,146,40348.390046*18",
                "#0,4,-53.8,5,18.95,6,266.2,9,320.0*3A", // invalid
                "#0,9,320.0*05",
                "#0,9,323.0*05", // invalid
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#1,9,320.0*05", // invalid
                "#0,9,320.0*05",
                "#0,9,320.0*05",
                "#0,1,7.700,2,-36.0,3,25.10,4,-49.5,5,19.41,6,271.5,9,321.0,12,1.07,50,327.3,146,40348.390058*10",
                "#0,9,321.0*04"
        };
        buf = new byte[512];
        packet = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), PORT);
        socket = new DatagramSocket();
        receiver = new UDPExpeditionReceiver(PORT);
        receiverThread = new Thread(receiver, "Expedition Receiver");
        receiverThread.start();
        Thread.sleep(10); // to give receiver enough time to start listening on UDP socket
        listener = new ExpeditionListener() {
            @Override
            public void received(ExpeditionMessage message) {
                messages.add(message);
            }
        };
    }
    
    @After
    public void tearDown() {
        socket.close();
        receiver.removeListener(listener);
    }
    
    @Test
    public void sendAndValidateValidDatagrams() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ false);
        sendAndWaitABit(validLines);
        assertEquals(validLines.length, messages.size());
        ExpeditionMessage m = messages.get(0);
        assertEquals(0, m.getBoatID());
        assertTrue(m.hasValue(1));
        assertEquals(7.700, m.getValue(1), 0.00000001);
        assertTrue(m.hasValue(2));
        assertEquals(-39.0, m.getValue(2), 0.00000001);
        assertTrue(m.hasValue(3));
        assertEquals(23.00, m.getValue(3), 0.00000001);
        assertTrue(m.hasValue(9));
        assertEquals(319.0, m.getValue(9), 0.00000001);
        assertTrue(m.hasValue(146));
        assertEquals(40348.390035, m.getValue(146), 0.00000001);
    }
    
    @Test
    public void testTwoBoatsAtDifferentPositionsWithSporadicMissingPosition() throws IOException, InterruptedException {
        final List<Wind> windFixes = new ArrayList<Wind>();
        ExpeditionWindTracker windTracker = new ExpeditionWindTracker(new MockedTrackedRace() {
            private static final long serialVersionUID = 4444197492014940699L;
            @Override
            public boolean recordWind(Wind wind, WindSource windSource) {
                windFixes.add(wind);
                return true;
            }
        }, null, receiver, /* ExpeditionWindTrackerFactory */ null);
        receiver.addListener(windTracker, /* validMessagesOnly */ false);
        receiver.addListener(listener, /* validMessagesOnly */ false);
        sendAndWaitABit(new String[] { "#0,1,7.900,2,-42.0,3,25.90,5,19.41,6,271.5,9,323.0,13,326.0,48,50.000000,49,10.000000,50,340.3,146,40348.578310*25",
                "#1,1,7.800,2,-42.0,3,24.80,5,19.41,6,271.5,9,323.0,13,326.0,48,54.000000,49,12.000000,50,340.3,146,40348.578311*25",
                "#0,1,7.700,2,-42.0,3,23.70,5,19.41,6,271.5,9,320.0,13,322.0,50,343.3,146,40348.578312*25" });
        assertEquals(3, messages.size());
        assertEquals(3, windFixes.size());
        assertEquals(messages.get(0).getBoatID(), messages.get(2).getBoatID());
        assertTrue(messages.get(2).hasValue(ExpeditionMessage.ID_GPS_TIME));
        assertFalse(messages.get(2).hasValue(ExpeditionMessage.ID_GPS_LAT));
        assertFalse(messages.get(2).hasValue(ExpeditionMessage.ID_GPS_LNG));
        assertEquals(windFixes.get(0).getPosition(), windFixes.get(2).getPosition());
    }

    @Test
    public void testBasicPhoenixUDPProperties() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ true);
        final InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("/Expedition_28Oct17_0820.txt"));
        final BufferedReader br = new BufferedReader(reader);
        final List<String> lines = new ArrayList<>();
        String line;
        while ((line=br.readLine()) != null) {
            if (!line.trim().isEmpty()) {
                lines.add(line.trim());
            }
        }
        br.close();
        sendAndWaitABit(lines.toArray(new String[0]));
        assertEquals(159, lines.size());
        assertEquals(159, messages.size());
    }

    @Test
    public void testTimeStampConversion() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ true);
        sendAndWaitABit(new String[] { "#0,1,7.900,2,-42.0,3,25.90,9,323.0,13,326.0,48,54.511867,49,10.152700,50,340.3,146,40348.578310*25" });
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).hasValue(ExpeditionMessage.ID_GPS_TIME));
        assertTrue(messages.get(0).hasValue(ExpeditionMessage.ID_GPS_LAT));
        assertTrue(messages.get(0).hasValue(ExpeditionMessage.ID_GPS_LNG));
        GPSFix fix = messages.get(0).getGPSFix();
        assertNotNull(fix);
        TimePoint time = fix.getTimePoint();
        Date date = time.asDate();
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        assertEquals(2010, cal.get(Calendar.YEAR));
        assertEquals(5, cal.get(Calendar.MONTH));
        assertEquals(19, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(52, cal.get(Calendar.MINUTE));
        // for some bizarre reason the conversion doesn't seem to be predictable; a calendar / platform issue?
        assertTrue(cal.get(Calendar.SECOND) == 46 || cal.get(Calendar.SECOND) == 45);
    }

    /**
     * If no timestamp is received in a message, by default the time the message was received is used to fill it in.
     */
    @Test
    public void testTimeStampCompletionFromCurrentTime() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ true);
        sendAndWaitABit(new String[] { "#0,9,318.0*0E" });
        assertEquals(1, messages.size());
        assertFalse(messages.get(0).hasValue(ExpeditionMessage.ID_GPS_TIME));
        assertEquals(messages.get(0).getCreatedAt(), messages.get(0).getTimePoint());
    }

    /**
     * If no timestamp is received in a message, by default the time the message was received is used to fill it in.
     */
    @Test
    public void testTimeStampCompletionFromLastTimePointReceived() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ true);
        sendAndWaitABit(validLines);
        assertEquals(validLines.length, messages.size());
        assertTrue(messages.get(0).hasValue(ExpeditionMessage.ID_GPS_TIME));
        assertTrue(messages.get(10).hasValue(ExpeditionMessage.ID_GPS_TIME));
        TimePoint m0Time = messages.get(0).getTimePoint();
        TimePoint m10Time = messages.get(10).getTimePoint();
        for (int i=1; i<=9; i++) {
            TimePoint m_iTime = messages.get(i).getTimePoint();
            assertTrue(m_iTime.compareTo(m0Time) >= 0);
            assertTrue(m_iTime.compareTo(m10Time) <= 0);
        }
    }

    @Test
    public void sendAndValidateSomeInvalidDatagrams() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ true);
        sendAndWaitABit(someValidWithFourInvalidLines);
        assertEquals(someValidWithFourInvalidLines.length-4 /* assuming 4 lines are invalid */, messages.size());
    }

    @Test
    public void sendAndValidateSomeInvalidDatagramsAcceptingInvalid() throws IOException, InterruptedException {
        receiver.addListener(listener, /* validMessagesOnly */ false);
        sendAndWaitABit(someValidWithFourInvalidLines);
        assertEquals(someValidWithFourInvalidLines.length, messages.size());
    }

    private void sendAndWaitABit(String[] linesToSend) throws IOException, InterruptedException {
        for (String line : linesToSend) {
            byte[] lineAsBytes = line.getBytes();
            System.arraycopy(lineAsBytes, 0, buf, 0, lineAsBytes.length);
            packet.setLength(lineAsBytes.length);
            socket.send(packet);
        }
        Thread.sleep(2000 /* ms */); // wait until all data was received
        receiver.stop();
        receiverThread.join(); // ensure the received has cleaned up and closed its socket
    }
    
    @Test
    public void testWindTrackerWithDeclination() throws IOException, InterruptedException, ClassNotFoundException, ParseException {
        MockedTrackedRace race = new MockedTrackedRace();
        DeclinationService declinationService = DeclinationService.INSTANCE;
        ExpeditionWindTracker windTracker = new ExpeditionWindTracker(race, declinationService, receiver,
                (ExpeditionTrackerFactory) ExpeditionTrackerFactory.getInstance());
        receiver.addListener(listener, /* validMessagesOnly */ true);
        receiver.addListener(windTracker, /* validMessagesOnly */ true);
        String[] lines = new String[validLines.length+1];
        lines[0] = "#0,1,7.900,2,-42.0,3,25.90,9,323.0,13,326.0,48,54.511867,49,10.152700,50,340.3,146,40348.578310*25";
        System.arraycopy(validLines, 0, lines, 1, validLines.length);
        // ensure declination service has 2011 loaded (which takes a few seconds)
        declinationService.getDeclination(
                new MillisecondsTimePoint(new SimpleDateFormat("yyyy-MM-dd").parse("2011-07-01").getTime()),
                new DegreePosition(54, 9), /* timeoutForOnlineFetchInMilliseconds */3000);
        sendAndWaitABit(lines);
        Thread.sleep(3000); // wait until at least the declination was received
        assertEquals(lines.length, messages.size());
        // note that the tracks are ordered by timestamps; however, not all Expedition messages have an original timestamp.
        // So, some of them are timestamped with "now" which shuffles ordering. We keep track of the matched wind fixes in
        // a map
        Set<Wind> matched = new HashSet<Wind>();
        // now assert that wind bearings have undergone declination correction
        Position lastKnownPosition = null;
        for (ExpeditionMessage m : messages) {
            if (m.getGPSFix() != null) {
                lastKnownPosition = m.getGPSFix().getPosition();
            }
            if (m.getTrueWind() != null) {
                Declination declination = declinationService.getDeclination(m.getTimePoint(), lastKnownPosition,
                        /* timeoutForOnlineFetchInMilliseconds */5000);
                race.getWindTrack().lockForRead();
                try {
                    for (Wind recordedWind : race.getWindTrack().getRawFixes()) {
                        if (Math.abs(m.getTrueWindBearing().getDegrees()
                                + declination.getBearingCorrectedTo(m.getTimePoint()).getDegrees()
                                - recordedWind.getBearing().getDegrees()) <= 0.0000001) {
                            matched.add(recordedWind);
                            break;
                        }
                    }
                } finally {
                    race.getWindTrack().unlockAfterRead();
                }
            }
        }
        race.getWindTrack().lockForRead();
        try {
            assertEquals(Util.size(race.getWindTrack().getFixes()), matched.size());
        } finally {
            race.getWindTrack().unlockAfterRead();
        }
    }
}
