package com.sap.sailing.domain.swisstimingadapter.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.MessageType;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceStatus;
import com.sap.sailing.domain.swisstimingadapter.RacingStatus;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterListener;
import com.sap.sailing.domain.swisstimingadapter.SailMasterMessage;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.impl.RaceImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

@Timeout(value=5, unit=TimeUnit.MINUTES)
public class SailMasterConnectivityTest {
    private static final Logger logger = Logger.getLogger(SailMasterConnectivityTest.class.getName());
    
    private static final int port = 24354;
    
    private Thread dummyServerThread;
    private SailMasterDummy sailMaster;

    private SailMasterConnector connector4702;
    
    private SailMasterConnector connector4711;

    private SailMasterConnector connector4712;

    @BeforeEach
    public void setUp() throws InterruptedException, ParseException {
        startSailMasterDummy();
        Race race4702 = new RaceImpl("W4702", "R2", "470 Women Race 2");
        Race race4711 = new RaceImpl("4711", "R2", "A wonderful test race");
        Race race4712 = new RaceImpl("4712", "R2", "Not such a wonderful race");
        connector4702 = SwissTimingFactory.INSTANCE.getOrCreateSailMasterConnector("localhost", port, race4702.getRaceID(), /* raceDataUrl */ null, race4702.getRaceName(), race4702.getDescription(), null /* boat class */, /* SwissTimingRaceTracker */ null);
        connector4711 = SwissTimingFactory.INSTANCE.getOrCreateSailMasterConnector("localhost", port, race4711.getRaceID(), /* raceDataUrl */ null, race4702.getRaceName(), race4711.getDescription(), null /* boat class */, /* SwissTimingRaceTracker */ null);
        connector4712 = SwissTimingFactory.INSTANCE.getOrCreateSailMasterConnector("localhost", port, race4712.getRaceID(), /* raceDataUrl */ null, race4702.getRaceName(), race4712.getDescription(), null /* boat class */, /* SwissTimingRaceTracker */ null);
        Thread.sleep(1000); // wait until all connectors are really connected to the SailMasterDummy
    }

    private void startSailMasterDummy() throws InterruptedException {
        // create a SailMaster dummy on port 50002 and launch it in a separate thread
        sailMaster = new SailMasterDummy(port);
        dummyServerThread = new Thread(sailMaster, "SailMasterDummy on port "+port);
        dummyServerThread.start();
        Thread.sleep(100); // give dummy sail master server a change to start listening on its socket
    }
    
    @AfterEach
    public void tearDown() throws IOException, InterruptedException {
        connector4702.sendRequestAndGetResponse(MessageType._STOPSERVER);
        connector4711.stop();
        connector4712.stop();
        try {
            Socket s = new Socket("localhost", port); // ensure that the stopped state is recognized and the dummy
                                                      // server doesn't wait forever in the accept
            s.close();
        } catch (ConnectException e) {
            // that's ok then; the server socket was obviously already closed properly and didn't need this ping
        }
        dummyServerThread.join();
        Thread.sleep(500); // give socket subsystem a chance to free up the port
    }
    
    @Test
    public void testRPD() throws IOException, InterruptedException {
        final String[] raceIDResult = new String[1];
        final RaceStatus[] raceStatusResult = new RaceStatus[1];
        final Collection<Fix> fixesResult = new ArrayList<Fix>();
        connector4702.addSailMasterListener(new SailMasterListener() {
            @Override
            public void receivedTimingData(String raceID, String boatID, List<com.sap.sse.common.Util.Triple<Integer, Integer, Long>> markIndicesRanksAndTimesSinceStartInMilliseconds) {}
            @Override
            public void receivedStartList(String raceID, StartList startList) {}
            @Override
            public void receivedCourseConfiguration(String raceID, Course course) {}
            @Override
            public void receivedClockAtMark(String raceID, List<com.sap.sse.common.Util.Triple<Integer, TimePoint, String>> markIndicesTimePointsAndBoatIDs) {}
            @Override
            public void receivedAvailableRaces(Iterable<Race> races) {}
            @Override
            public void storedDataProgress(String raceID, double progress, TrackedRaceStatusEnum statusAfterLoadingComplete) {}
            @Override
            public void receivedWindData(String raceID, int zeroBasedMarkIndex, double windDirectionTrueDegrees, double windSpeedInKnots) {}
            @Override
            public void receivedRacePositionData(String raceID, RaceStatus raceStatus, RacingStatus racingStatus, TimePoint timePoint,
                    TimePoint startTime, Long millisecondsSinceRaceStart, Integer nextMarkIndexForLeader,
                    Distance distanceToNextMarkForLeaderInMeters, Collection<Fix> fixes) {
                raceIDResult[0] = raceID;
                raceStatusResult[0] = raceStatus;
                fixesResult.addAll(fixes);
                synchronized (SailMasterConnectivityTest.this) {
                    SailMasterConnectivityTest.this.notifyAll();
                }
            }
        });
        synchronized (this) {
            sailMaster.sendEvent("RPD|W4702|0|2011-06-06T18:16:40+02:00|||||61|SB;2;0;50.621906;-2.366133;0.2;;;90;;;;|MB4;4;0;50.604516;-2.373371;2.1;;;108;;;;|MB3;4;0;50.607238;-2.388115;5.4;;;218;;;;|MB2;4;0;50.618063;-2.352200;3.5;;;150;;;;|MB1;4;0;50.620284;-2.366468;0.6;;;0;;;;|M7;5;0;50.621875;-2.366162;0.0;;;90;;;;|M6;5;0;50.607593;-2.387875;0.6;;;0;;;;|M5;5;0;50.605033;-2.373561;0.8;;;90;;;;|M4;5;0;50.617114;-2.352124;0.4;;;90;;;;|M3;5;0;50.620244;-2.366878;0.0;;;180;;;;|M2;5;0;50.619857;-2.366341;1.0;;;90;;;;|M1;5;0;50.617371;-2.352767;1.4;;;90;;;;|FP;4;0;50.619979;-2.354295;1.2;;;90;;;;|FB;4;0;50.619997;-2.354945;0.0;;;90;;;;|SP;4;0;50.610540;-2.347954;4.3;;;276;;;;|ARG_8;1;0;50.609389;-2.378625;4.3;;;70;;;;|AUS_7;1;0;50.607808;-2.377885;5.1;;;79;;;;|AUT_431;1;0;50.609798;-2.375270;5.6;;;60;;;;|BRA_177;1;0;50.611476;-2.379518;5.8;;;63;;;;|BRA_1187;1;0;50.611936;-2.379847;6.0;;;71;;;;|CAN_11;1;0;50.608996;-2.378944;4.3;;;63;;;;|CAN_610;1;0;50.609465;-2.380589;5.8;;;78;;;;|CHN_1211;1;0;50.609146;-2.379306;5.8;;;55;;;;|CHN_1261;1;27;50.609774;-2.381628;5.4;;;44;;;;|CHN_221;1;0;50.611029;-2.376609;5.4;;;82;;;;|CHN_616;1;0;50.611100;-2.375881;5.1;;;73;;;;|CRO_111;1;0;50.610189;-2.376963;5.1;;;72;;;;|DEN_143;1;0;50.609804;-2.375818;4.9;;;49;;;;|ESP_133;1;0;50.609543;-2.380252;6.0;;;54;;;;|ESP_696;1;0;50.609737;-2.375615;4.3;;;54;;;;|EST_20;1;0;50.609752;-2.378286;4.7;;;25;;;;|FRA_12;1;0;50.612187;-2.381923;5.4;;;32;;;;|FRA_4;1;0;50.609405;-2.378963;5.8;;;81;;;;|FRA_9;1;0;50.608222;-2.377562;5.1;;;59;;;;|GBR_812;1;0;50.608942;-2.378740;5.8;;;81;;;;|GBR_831;1;;;;;;;;;;;|GBR_841;1;0;50.610339;-2.378788;4.1;;;84;;;;|GBR_847;1;0;50.609013;-2.378316;4.9;;;61;;;;|GBR_850;1;0;50.609983;-2.376479;5.2;;;66;;;;|GBR_855;1;0;50.610087;-2.377418;4.9;;;61;;;;|GER_12;1;0;50.608424;-2.378020;5.1;;;90;;;;|GER_21;1;0;50.608690;-2.379355;5.2;;;75;;;;|GER_61;1;0;50.612197;-2.380552;5.1;;;72;;;;|GER_66;1;0;50.610299;-2.379494;5.6;;;56;;;;|GER_72;1;0;50.611263;-2.382863;4.9;;;41;;;;|ISR_311;1;0;50.611931;-2.378758;5.2;;;48;;;;|ITA_23;1;0;50.611871;-2.376589;5.1;;;10;;;;|JPN_4151;1;0;50.610487;-2.376647;5.1;;;17;;;;|JPN_4321;1;0;50.609067;-2.378408;5.8;;;63;;;;|NED_11;1;0;50.609497;-2.374946;5.4;;;94;;;;|NED_24;1;;;;;;;;;;;|NZL_75;1;0;50.609794;-2.376366;5.6;;;51;;;;|RUS_12;1;0;50.609904;-2.380138;5.1;;;56;;;;|RUS_700;1;0;50.609864;-2.381247;4.7;;;29;;;;|SLO_64;1;0;50.611275;-2.380638;5.2;;;53;;;;|SUI_14;1;0;50.611303;-2.382679;7.6;;;32;;;;|SWE_342;1;0;50.608294;-2.377717;4.7;;;96;;;;|SWE_344;1;0;50.609573;-2.375109;6.0;;;77;;;;|USA_1712;1;0;50.609414;-2.379397;5.8;;;85;;;;|USA_1736;1;0;50.611837;-2.380319;5.4;;;72;;;;|USA_1757;1;0;50.610240;-2.376824;5.8;;;9;;;;");
            this.wait();
        }
        assertEquals("W4702", raceIDResult[0]);
        assertEquals(RaceStatus.READY, raceStatusResult[0]);
        assertEquals(59, fixesResult.size());
    }
    
    @Test
    public void testRPDWithRacingStatus() throws IOException, InterruptedException {
        final String[] raceIDResult = new String[1];
        final RaceStatus[] raceStatusResult = new RaceStatus[1];
        final RacingStatus[] racingStatusResult = new RacingStatus[1];
        final Collection<Fix> fixesResult = new ArrayList<Fix>();
        connector4702.addSailMasterListener(new SailMasterListener() {
            @Override
            public void receivedTimingData(String raceID, String boatID, List<com.sap.sse.common.Util.Triple<Integer, Integer, Long>> markIndicesRanksAndTimesSinceStartInMilliseconds) {}
            @Override
            public void receivedStartList(String raceID, StartList startList) {}
            @Override
            public void receivedCourseConfiguration(String raceID, Course course) {}
            @Override
            public void receivedClockAtMark(String raceID, List<com.sap.sse.common.Util.Triple<Integer, TimePoint, String>> markIndicesTimePointsAndBoatIDs) {}
            @Override
            public void receivedAvailableRaces(Iterable<Race> races) {}
            @Override
            public void storedDataProgress(String raceID, double progress, TrackedRaceStatusEnum statusAfterLoadingComplete) {}
            @Override
            public void receivedWindData(String raceID, int zeroBasedMarkIndex, double windDirectionTrueDegrees, double windSpeedInKnots) {}
            @Override
            public void receivedRacePositionData(String raceID, RaceStatus raceStatus, RacingStatus racingStatus, TimePoint timePoint,
                    TimePoint startTime, Long millisecondsSinceRaceStart, Integer nextMarkIndexForLeader,
                    Distance distanceToNextMarkForLeaderInMeters, Collection<Fix> fixes) {
                raceIDResult[0] = raceID;
                raceStatusResult[0] = raceStatus;
                racingStatusResult[0] = racingStatus;
                fixesResult.addAll(fixes);
                synchronized (SailMasterConnectivityTest.this) {
                    SailMasterConnectivityTest.this.notifyAll();
                }
            }
        });
        synchronized (this) {
            sailMaster.sendEvent("RPD|W4702|0,3|2011-06-06T18:16:40+02:00|||||61|SB;2;0;50.621906;-2.366133;0.2;;;90;;;;|MB4;4;0;50.604516;-2.373371;2.1;;;108;;;;|MB3;4;0;50.607238;-2.388115;5.4;;;218;;;;|MB2;4;0;50.618063;-2.352200;3.5;;;150;;;;|MB1;4;0;50.620284;-2.366468;0.6;;;0;;;;|M7;5;0;50.621875;-2.366162;0.0;;;90;;;;|M6;5;0;50.607593;-2.387875;0.6;;;0;;;;|M5;5;0;50.605033;-2.373561;0.8;;;90;;;;|M4;5;0;50.617114;-2.352124;0.4;;;90;;;;|M3;5;0;50.620244;-2.366878;0.0;;;180;;;;|M2;5;0;50.619857;-2.366341;1.0;;;90;;;;|M1;5;0;50.617371;-2.352767;1.4;;;90;;;;|FP;4;0;50.619979;-2.354295;1.2;;;90;;;;|FB;4;0;50.619997;-2.354945;0.0;;;90;;;;|SP;4;0;50.610540;-2.347954;4.3;;;276;;;;|ARG_8;1;0;50.609389;-2.378625;4.3;;;70;;;;|AUS_7;1;0;50.607808;-2.377885;5.1;;;79;;;;|AUT_431;1;0;50.609798;-2.375270;5.6;;;60;;;;|BRA_177;1;0;50.611476;-2.379518;5.8;;;63;;;;|BRA_1187;1;0;50.611936;-2.379847;6.0;;;71;;;;|CAN_11;1;0;50.608996;-2.378944;4.3;;;63;;;;|CAN_610;1;0;50.609465;-2.380589;5.8;;;78;;;;|CHN_1211;1;0;50.609146;-2.379306;5.8;;;55;;;;|CHN_1261;1;27;50.609774;-2.381628;5.4;;;44;;;;|CHN_221;1;0;50.611029;-2.376609;5.4;;;82;;;;|CHN_616;1;0;50.611100;-2.375881;5.1;;;73;;;;|CRO_111;1;0;50.610189;-2.376963;5.1;;;72;;;;|DEN_143;1;0;50.609804;-2.375818;4.9;;;49;;;;|ESP_133;1;0;50.609543;-2.380252;6.0;;;54;;;;|ESP_696;1;0;50.609737;-2.375615;4.3;;;54;;;;|EST_20;1;0;50.609752;-2.378286;4.7;;;25;;;;|FRA_12;1;0;50.612187;-2.381923;5.4;;;32;;;;|FRA_4;1;0;50.609405;-2.378963;5.8;;;81;;;;|FRA_9;1;0;50.608222;-2.377562;5.1;;;59;;;;|GBR_812;1;0;50.608942;-2.378740;5.8;;;81;;;;|GBR_831;1;;;;;;;;;;;|GBR_841;1;0;50.610339;-2.378788;4.1;;;84;;;;|GBR_847;1;0;50.609013;-2.378316;4.9;;;61;;;;|GBR_850;1;0;50.609983;-2.376479;5.2;;;66;;;;|GBR_855;1;0;50.610087;-2.377418;4.9;;;61;;;;|GER_12;1;0;50.608424;-2.378020;5.1;;;90;;;;|GER_21;1;0;50.608690;-2.379355;5.2;;;75;;;;|GER_61;1;0;50.612197;-2.380552;5.1;;;72;;;;|GER_66;1;0;50.610299;-2.379494;5.6;;;56;;;;|GER_72;1;0;50.611263;-2.382863;4.9;;;41;;;;|ISR_311;1;0;50.611931;-2.378758;5.2;;;48;;;;|ITA_23;1;0;50.611871;-2.376589;5.1;;;10;;;;|JPN_4151;1;0;50.610487;-2.376647;5.1;;;17;;;;|JPN_4321;1;0;50.609067;-2.378408;5.8;;;63;;;;|NED_11;1;0;50.609497;-2.374946;5.4;;;94;;;;|NED_24;1;;;;;;;;;;;|NZL_75;1;0;50.609794;-2.376366;5.6;;;51;;;;|RUS_12;1;0;50.609904;-2.380138;5.1;;;56;;;;|RUS_700;1;0;50.609864;-2.381247;4.7;;;29;;;;|SLO_64;1;0;50.611275;-2.380638;5.2;;;53;;;;|SUI_14;1;0;50.611303;-2.382679;7.6;;;32;;;;|SWE_342;1;0;50.608294;-2.377717;4.7;;;96;;;;|SWE_344;1;0;50.609573;-2.375109;6.0;;;77;;;;|USA_1712;1;0;50.609414;-2.379397;5.8;;;85;;;;|USA_1736;1;0;50.611837;-2.380319;5.4;;;72;;;;|USA_1757;1;0;50.610240;-2.376824;5.8;;;9;;;;");
            this.wait();
        }
        assertEquals("W4702", raceIDResult[0]);
        assertEquals(RaceStatus.READY, raceStatusResult[0]);
        assertEquals(RacingStatus.FINISHED, racingStatusResult[0]);
        assertEquals(59, fixesResult.size());
    }
    
    @Test
    public void testRAC() throws UnknownHostException, IOException, InterruptedException {
        // TODO this test probably doesn't make sense anymore because request/response is no longer supported
        SailMasterMessage response = connector4711.sendRequestAndGetResponse(MessageType.RAC);
        assertEquals(MessageType.RAC.name()+"!|2|4711;A wonderful test race|4712;Not such a wonderful race", response.getMessage());
        assertArrayEquals(new String[] { "RAC!", "2", "4711;A wonderful test race",
                "4712;Not such a wonderful race" }, response.getSections());
        assertArrayEquals(new Object[] { "4711", "A wonderful test race" }, response.getSections()[2].split(";"));
        assertArrayEquals(new Object[] { "4712", "Not such a wonderful race" }, response.getSections()[3].split(";"));
    }
    
    @Test
    public void testRaceTime() throws UnknownHostException, IOException, ParseException, InterruptedException {
        logger.info("Starting testRaceTime");
        final String timeZoneOffset = "+02:00";
        sailMaster.sendEvent("RPD|4711|0|2011-06-06T18:16:40"+timeZoneOffset+"|10:15:22|00:10:00|||61|SB;2;0;50.621906;-2.366133;0.2;;;90;;;;|MB4;4;0;50.604516;-2.373371;2.1;;;108;;;;|MB3;4;0;50.607238;-2.388115;5.4;;;218;;;;|MB2;4;0;50.618063;-2.352200;3.5;;;150;;;;|MB1;4;0;50.620284;-2.366468;0.6;;;0;;;;|M7;5;0;50.621875;-2.366162;0.0;;;90;;;;|M6;5;0;50.607593;-2.387875;0.6;;;0;;;;|M5;5;0;50.605033;-2.373561;0.8;;;90;;;;|M4;5;0;50.617114;-2.352124;0.4;;;90;;;;|M3;5;0;50.620244;-2.366878;0.0;;;180;;;;|M2;5;0;50.619857;-2.366341;1.0;;;90;;;;|M1;5;0;50.617371;-2.352767;1.4;;;90;;;;|FP;4;0;50.619979;-2.354295;1.2;;;90;;;;|FB;4;0;50.619997;-2.354945;0.0;;;90;;;;|SP;4;0;50.610540;-2.347954;4.3;;;276;;;;|ARG_8;1;0;50.609389;-2.378625;4.3;;;70;;;;|AUS_7;1;0;50.607808;-2.377885;5.1;;;79;;;;|AUT_431;1;0;50.609798;-2.375270;5.6;;;60;;;;|BRA_177;1;0;50.611476;-2.379518;5.8;;;63;;;;|BRA_1187;1;0;50.611936;-2.379847;6.0;;;71;;;;|CAN_11;1;0;50.608996;-2.378944;4.3;;;63;;;;|CAN_610;1;0;50.609465;-2.380589;5.8;;;78;;;;|CHN_1211;1;0;50.609146;-2.379306;5.8;;;55;;;;|CHN_1261;1;27;50.609774;-2.381628;5.4;;;44;;;;|CHN_221;1;0;50.611029;-2.376609;5.4;;;82;;;;|CHN_616;1;0;50.611100;-2.375881;5.1;;;73;;;;|CRO_111;1;0;50.610189;-2.376963;5.1;;;72;;;;|DEN_143;1;0;50.609804;-2.375818;4.9;;;49;;;;|ESP_133;1;0;50.609543;-2.380252;6.0;;;54;;;;|ESP_696;1;0;50.609737;-2.375615;4.3;;;54;;;;|EST_20;1;0;50.609752;-2.378286;4.7;;;25;;;;|FRA_12;1;0;50.612187;-2.381923;5.4;;;32;;;;|FRA_4;1;0;50.609405;-2.378963;5.8;;;81;;;;|FRA_9;1;0;50.608222;-2.377562;5.1;;;59;;;;|GBR_812;1;0;50.608942;-2.378740;5.8;;;81;;;;|GBR_831;1;;;;;;;;;;;|GBR_841;1;0;50.610339;-2.378788;4.1;;;84;;;;|GBR_847;1;0;50.609013;-2.378316;4.9;;;61;;;;|GBR_850;1;0;50.609983;-2.376479;5.2;;;66;;;;|GBR_855;1;0;50.610087;-2.377418;4.9;;;61;;;;|GER_12;1;0;50.608424;-2.378020;5.1;;;90;;;;|GER_21;1;0;50.608690;-2.379355;5.2;;;75;;;;|GER_61;1;0;50.612197;-2.380552;5.1;;;72;;;;|GER_66;1;0;50.610299;-2.379494;5.6;;;56;;;;|GER_72;1;0;50.611263;-2.382863;4.9;;;41;;;;|ISR_311;1;0;50.611931;-2.378758;5.2;;;48;;;;|ITA_23;1;0;50.611871;-2.376589;5.1;;;10;;;;|JPN_4151;1;0;50.610487;-2.376647;5.1;;;17;;;;|JPN_4321;1;0;50.609067;-2.378408;5.8;;;63;;;;|NED_11;1;0;50.609497;-2.374946;5.4;;;94;;;;|NED_24;1;;;;;;;;;;;|NZL_75;1;0;50.609794;-2.376366;5.6;;;51;;;;|RUS_12;1;0;50.609904;-2.380138;5.1;;;56;;;;|RUS_700;1;0;50.609864;-2.381247;4.7;;;29;;;;|SLO_64;1;0;50.611275;-2.380638;5.2;;;53;;;;|SUI_14;1;0;50.611303;-2.382679;7.6;;;32;;;;|SWE_342;1;0;50.608294;-2.377717;4.7;;;96;;;;|SWE_344;1;0;50.609573;-2.375109;6.0;;;77;;;;|USA_1712;1;0;50.609414;-2.379397;5.8;;;85;;;;|USA_1736;1;0;50.611837;-2.380319;5.4;;;72;;;;|USA_1757;1;0;50.610240;-2.376824;5.8;;;9;;;;");
        Thread.sleep(500);
        TimePoint start1 = connector4711.getStartTime();
        Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"+timeZoneOffset));
        cal.setTime(start1.asDate());
        assertEquals(2011, cal.get(Calendar.YEAR));
        assertEquals(5, cal.get(Calendar.MONTH));
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH));
        // 10:15:22
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal.get(Calendar.MINUTE));
        assertEquals(22, cal.get(Calendar.SECOND));
        sailMaster.sendEvent("RPD|4712|0|2011-06-06T18:16:40"+timeZoneOffset+"|18:17:23|00:10:00|||61|SB;2;0;50.621906;-2.366133;0.2;;;90;;;;|MB4;4;0;50.604516;-2.373371;2.1;;;108;;;;|MB3;4;0;50.607238;-2.388115;5.4;;;218;;;;|MB2;4;0;50.618063;-2.352200;3.5;;;150;;;;|MB1;4;0;50.620284;-2.366468;0.6;;;0;;;;|M7;5;0;50.621875;-2.366162;0.0;;;90;;;;|M6;5;0;50.607593;-2.387875;0.6;;;0;;;;|M5;5;0;50.605033;-2.373561;0.8;;;90;;;;|M4;5;0;50.617114;-2.352124;0.4;;;90;;;;|M3;5;0;50.620244;-2.366878;0.0;;;180;;;;|M2;5;0;50.619857;-2.366341;1.0;;;90;;;;|M1;5;0;50.617371;-2.352767;1.4;;;90;;;;|FP;4;0;50.619979;-2.354295;1.2;;;90;;;;|FB;4;0;50.619997;-2.354945;0.0;;;90;;;;|SP;4;0;50.610540;-2.347954;4.3;;;276;;;;|ARG_8;1;0;50.609389;-2.378625;4.3;;;70;;;;|AUS_7;1;0;50.607808;-2.377885;5.1;;;79;;;;|AUT_431;1;0;50.609798;-2.375270;5.6;;;60;;;;|BRA_177;1;0;50.611476;-2.379518;5.8;;;63;;;;|BRA_1187;1;0;50.611936;-2.379847;6.0;;;71;;;;|CAN_11;1;0;50.608996;-2.378944;4.3;;;63;;;;|CAN_610;1;0;50.609465;-2.380589;5.8;;;78;;;;|CHN_1211;1;0;50.609146;-2.379306;5.8;;;55;;;;|CHN_1261;1;27;50.609774;-2.381628;5.4;;;44;;;;|CHN_221;1;0;50.611029;-2.376609;5.4;;;82;;;;|CHN_616;1;0;50.611100;-2.375881;5.1;;;73;;;;|CRO_111;1;0;50.610189;-2.376963;5.1;;;72;;;;|DEN_143;1;0;50.609804;-2.375818;4.9;;;49;;;;|ESP_133;1;0;50.609543;-2.380252;6.0;;;54;;;;|ESP_696;1;0;50.609737;-2.375615;4.3;;;54;;;;|EST_20;1;0;50.609752;-2.378286;4.7;;;25;;;;|FRA_12;1;0;50.612187;-2.381923;5.4;;;32;;;;|FRA_4;1;0;50.609405;-2.378963;5.8;;;81;;;;|FRA_9;1;0;50.608222;-2.377562;5.1;;;59;;;;|GBR_812;1;0;50.608942;-2.378740;5.8;;;81;;;;|GBR_831;1;;;;;;;;;;;|GBR_841;1;0;50.610339;-2.378788;4.1;;;84;;;;|GBR_847;1;0;50.609013;-2.378316;4.9;;;61;;;;|GBR_850;1;0;50.609983;-2.376479;5.2;;;66;;;;|GBR_855;1;0;50.610087;-2.377418;4.9;;;61;;;;|GER_12;1;0;50.608424;-2.378020;5.1;;;90;;;;|GER_21;1;0;50.608690;-2.379355;5.2;;;75;;;;|GER_61;1;0;50.612197;-2.380552;5.1;;;72;;;;|GER_66;1;0;50.610299;-2.379494;5.6;;;56;;;;|GER_72;1;0;50.611263;-2.382863;4.9;;;41;;;;|ISR_311;1;0;50.611931;-2.378758;5.2;;;48;;;;|ITA_23;1;0;50.611871;-2.376589;5.1;;;10;;;;|JPN_4151;1;0;50.610487;-2.376647;5.1;;;17;;;;|JPN_4321;1;0;50.609067;-2.378408;5.8;;;63;;;;|NED_11;1;0;50.609497;-2.374946;5.4;;;94;;;;|NED_24;1;;;;;;;;;;;|NZL_75;1;0;50.609794;-2.376366;5.6;;;51;;;;|RUS_12;1;0;50.609904;-2.380138;5.1;;;56;;;;|RUS_700;1;0;50.609864;-2.381247;4.7;;;29;;;;|SLO_64;1;0;50.611275;-2.380638;5.2;;;53;;;;|SUI_14;1;0;50.611303;-2.382679;7.6;;;32;;;;|SWE_342;1;0;50.608294;-2.377717;4.7;;;96;;;;|SWE_344;1;0;50.609573;-2.375109;6.0;;;77;;;;|USA_1712;1;0;50.609414;-2.379397;5.8;;;85;;;;|USA_1736;1;0;50.611837;-2.380319;5.4;;;72;;;;|USA_1757;1;0;50.610240;-2.376824;5.8;;;9;;;;");
        Thread.sleep(500);
        TimePoint start2 = connector4712.getStartTime();
        cal.setTime(start2.asDate());
        assertEquals(2011, cal.get(Calendar.YEAR));
        assertEquals(5, cal.get(Calendar.MONTH));
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH));
        // 18:17:23
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(17, cal.get(Calendar.MINUTE));
        assertEquals(23, cal.get(Calendar.SECOND));
    }
    
    @Test
    public void testStructuredRaceId() throws UnknownHostException, IOException, InterruptedException {
        Race r1 = connector4711.getRace();
        assertEquals("4711", r1.getRaceID());
        assertEquals("A wonderful test race", r1.getDescription());
        Race r2 = connector4712.getRace();
        assertEquals("4712", r2.getRaceID());
        assertEquals("Not such a wonderful race", r2.getDescription());
    }

    @Test
    public void testCourseConfig() throws UnknownHostException, IOException, InterruptedException {
        Race r1 = connector4711.getRace();
        Course course1 = connector4711.getCourse(r1.getRaceID());
        assertEquals(r1.getRaceID(), course1.getRaceID());
        assertEquals(2, Util.size(course1.getMarks()));
        Iterator<Mark> mi = course1.getMarks().iterator();
        Mark m11 = mi.next();
        assertEquals("Lee Gate", m11.getDescription());
        assertEquals(Arrays.asList(new String[] { "LG1", "LG2" }), m11.getDeviceIds());
        assertEquals(1, m11.getIndex());
        Mark m12 = mi.next();
        assertEquals("Windward", m12.getDescription());
        assertEquals(Arrays.asList(new String[] { "WW1" }), m12.getDeviceIds());
        assertEquals(2, m12.getIndex());

        Race r2 = connector4712.getRace();
        Course course2 = connector4712.getCourse(r2.getRaceID());
        assertEquals(r2.getRaceID(), course2.getRaceID());
        assertEquals(3, Util.size(course2.getMarks()));
        mi = course2.getMarks().iterator();
        Mark m21 = mi.next();
        assertEquals("Lee Gate", m21.getDescription());
        assertEquals(Arrays.asList(new String[] { "LG1", "LG2" }), m21.getDeviceIds());
        assertEquals(1, m21.getIndex());
        Mark m22 = mi.next();
        assertEquals("Windward", m22.getDescription());
        assertEquals(Arrays.asList(new String[] { "WW1" }), m22.getDeviceIds());
        assertEquals(2, m22.getIndex());
        Mark m23 = mi.next();
        assertEquals("Offset", m23.getDescription());
        assertEquals(Arrays.asList(new String[] { "OS1" }), m23.getDeviceIds());
        assertEquals(3, m23.getIndex());
    }
    
    @Test
    public void testStartList() throws UnknownHostException, IOException, InterruptedException {
        Race r1 = connector4711.getRace();
        StartList sl1 = connector4711.getStartList(r1.getRaceID());
        assertEquals(r1.getRaceID(), sl1.getRaceID());
        assertEquals(2, Util.size(sl1.getCompetitors()));
        Iterator<Competitor> ci = sl1.getCompetitors().iterator();
        Competitor polgarKoy = ci.next();
        assertEquals("GER 8414", polgarKoy.getBoatID());
        assertEquals("GER", polgarKoy.getThreeLetterIOCCode());
        assertEquals("Polgar/Koy", polgarKoy.getName());
        Competitor schlonskiBohn = ci.next();
        assertEquals("GER 8140", schlonskiBohn.getBoatID());
        assertEquals("GER", schlonskiBohn.getThreeLetterIOCCode());
        assertEquals("Schlonski/Bohn", schlonskiBohn.getName());
        Race r2 = connector4712.getRace();
        StartList sl2 = connector4712.getStartList(r2.getRaceID());
        assertEquals(r2.getRaceID(), sl2.getRaceID());
        assertEquals(3, Util.size(sl2.getCompetitors()));
        ci = sl2.getCompetitors().iterator();
        Competitor stanjekKleen = ci.next();
        assertEquals("GER 8340", stanjekKleen.getBoatID());
        assertEquals("GER", stanjekKleen.getThreeLetterIOCCode());
        assertEquals("Stanjek/Kleen", stanjekKleen.getName());
        Competitor babendererdeJacobs = ci.next();
        assertEquals("GER 8433", babendererdeJacobs.getBoatID());
        assertEquals("GER", babendererdeJacobs.getThreeLetterIOCCode());
        assertEquals("Babendererde/Jacobs", babendererdeJacobs.getName());
        Competitor elsnerSchulz = ci.next();
        assertEquals("GER 8299", elsnerSchulz.getBoatID());
        assertEquals("GER", elsnerSchulz.getThreeLetterIOCCode());
        assertEquals("Elsner/Schulz", elsnerSchulz.getName());
    }
}
