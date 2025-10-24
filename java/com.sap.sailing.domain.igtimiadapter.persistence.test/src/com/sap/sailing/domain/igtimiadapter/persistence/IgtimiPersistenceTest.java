package com.sap.sailing.domain.igtimiadapter.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.igtimi.IgtimiData.ApparentWindSpeed;
import com.igtimi.IgtimiData.Data;
import com.igtimi.IgtimiData.DataMsg;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiData.DataPoint.DataCase;
import com.igtimi.IgtimiStream.Msg;
import com.mongodb.client.ClientSession;
import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Device;
import com.sap.sse.common.Duration;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.mongodb.MongoDBService;

public class IgtimiPersistenceTest {
    private static final Logger logger = Logger.getLogger(IgtimiPersistenceTest.class.getName());

    private static ClientSession clientSession;

    private static MongoDBConfiguration testDBConfig;

    private static MongoDBService mongoDBService;

    private MongoObjectFactory mongoObjectFactory;
    private DomainObjectFactory domainObjectFactory;
    
    @BeforeAll
    public static void setUpClientSession() {
        testDBConfig = MongoDBConfiguration.getDefaultTestConfiguration();
        mongoDBService = testDBConfig.getService();
        clientSession = mongoDBService.startCausallyConsistentSession();
    }
    
    @BeforeEach
    public void setUp() {
        mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(mongoDBService);
        mongoObjectFactory.clear(clientSession);
        domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(mongoDBService);
    }

    @Test
    public void testStoringAndLoadingSimpleDevice() {
        final Device device = Device.create(123, "DE-AC-AAHJ", "The tricky one");
        final String remoteAddress = new InetSocketAddress("127.0.0.1", 1234).toString();
        final TimePoint heartbeat = TimePoint.now();
        device.setLastHeartbeat(heartbeat, remoteAddress);
        mongoObjectFactory.storeDevice(device, clientSession);
        final Device loadedDevice = domainObjectFactory.getDevices(clientSession).iterator().next();
        assertEquals(device.getId(), loadedDevice.getId());
        assertEquals(device.getName(), loadedDevice.getName());
        assertEquals(device.getSerialNumber(), loadedDevice.getSerialNumber());
        assertEquals(new Util.Pair<>(heartbeat, remoteAddress), loadedDevice.getLastHeartbeat());
    }

    @Test
    public void testStoringAndLoadingSimpleDeviceWithEmptyHeartbeat() {
        final Device device = Device.create(123, "DE-AC-AAHJ", "The empty heartbeat");
        assertNull(device.getLastHeartbeat());
        mongoObjectFactory.storeDevice(device, clientSession);
        final Device loadedDevice = domainObjectFactory.getDevices(clientSession).iterator().next();
        assertEquals(device.getId(), loadedDevice.getId());
        assertEquals(device.getName(), loadedDevice.getName());
        assertEquals(device.getSerialNumber(), loadedDevice.getSerialNumber());
        assertNull(loadedDevice.getLastHeartbeat());
    }

    @Test
    public void testStoringAndLoadingSimpleDataAccessWindow() {
        final TimePoint from = TimePoint.now();
        final TimePoint to = from.plus(Duration.ONE_MINUTE);
        final DataAccessWindow daw = DataAccessWindow.create(345, from, to, "AA-CC-AAEH");
        mongoObjectFactory.storeDataAccessWindow(daw, clientSession);
        final DataAccessWindow loadedDaw = domainObjectFactory.getDataAccessWindows(clientSession).iterator().next();
        assertEquals(daw.getId(), loadedDaw.getId());
        assertEquals(daw.getName(), loadedDaw.getName()); // an inferred value; still useful to compare
        assertEquals(daw.getStartTime(), loadedDaw.getStartTime());
        assertEquals(daw.getEndTime(), loadedDaw.getEndTime());
        assertEquals(daw.getDeviceSerialNumber(), loadedDaw.getDeviceSerialNumber());
    }
    
    @Test
    public void testStoringAndLoadingSimpleProtobufMessage() {
        final TimePoint AWS_TIME_POINT = TimePoint.now();
        final String SERIAL_NUMBER = "DE-AA-AAHE";
        final int STREAM_ID = 358;
        final int AWS = 12;
        final Msg msg = Msg.newBuilder().setData(
                Data.newBuilder().addData(
                        DataMsg.newBuilder()
                            .setSerialNumber(SERIAL_NUMBER)
                            .setStreamId(STREAM_ID)
                            .addData(
                                    DataPoint.newBuilder()
                                        .setAws(ApparentWindSpeed.newBuilder()
                                                    .setTimestamp(AWS_TIME_POINT.asMillis())
                                                    .setValue(AWS)
                                                    .build())
                                        .build())
                            .build())
                    .build())
                .build();
        logger.info("Storing message: "+msg);
        mongoObjectFactory.storeMessage(SERIAL_NUMBER, msg, clientSession);
        final Set<DataCase> dataCases = new HashSet<>(Arrays.asList(DataCase.values()));
        final Iterable<Msg> result = domainObjectFactory.getMessages(SERIAL_NUMBER, MultiTimeRange.of(TimeRange.create(AWS_TIME_POINT.minus(Duration.ONE_MINUTE), AWS_TIME_POINT.plus(Duration.ONE_MINUTE))), dataCases, clientSession);
        final Iterator<Msg> iterator = result.iterator();
        assertTrue(iterator.hasNext());
        final Msg readMsg = iterator.next();
        assertFalse(iterator.hasNext());
        logger.info("Read message: "+readMsg);
        final Data data = readMsg.getData();
        final DataMsg dataMessage = data.getData(0);
        assertEquals(SERIAL_NUMBER, dataMessage.getSerialNumber());
        assertEquals(STREAM_ID, dataMessage.getStreamId());
        final DataPoint dataPoint = dataMessage.getData(0);
        final ApparentWindSpeed aws = dataPoint.getAws();
        assertEquals(AWS, aws.getValue(), 0.000000001);
        // search with a time range *before* the actual message
        assertTrue(Util.isEmpty(domainObjectFactory.getMessages(SERIAL_NUMBER, MultiTimeRange.of(TimeRange.create(AWS_TIME_POINT.minus(Duration.ONE_MINUTE.times(2)), AWS_TIME_POINT.minus(Duration.ONE_MINUTE))), dataCases, clientSession)));
        // search with a time range *after* the actual message
        assertTrue(Util.isEmpty(domainObjectFactory.getMessages(SERIAL_NUMBER, MultiTimeRange.of(TimeRange.create(AWS_TIME_POINT.plus(Duration.ONE_MINUTE), AWS_TIME_POINT.plus(Duration.ONE_MINUTE.times(2)))), dataCases, clientSession)));
        // search with the wrong device serial number
        assertTrue(Util.isEmpty(domainObjectFactory.getMessages(SERIAL_NUMBER+"-wrong", MultiTimeRange.of(TimeRange.create(AWS_TIME_POINT.minus(Duration.ONE_MINUTE), AWS_TIME_POINT.plus(Duration.ONE_MINUTE))), dataCases, clientSession)));
        // search with a time range open at the end
        assertFalse(Util.isEmpty(domainObjectFactory.getMessages(SERIAL_NUMBER, MultiTimeRange.of(TimeRange.create(AWS_TIME_POINT.minus(Duration.ONE_MINUTE), null)), dataCases, clientSession)));
        // search with a time range open at the start
        assertFalse(Util.isEmpty(domainObjectFactory.getMessages(SERIAL_NUMBER, MultiTimeRange.of(TimeRange.create(null, AWS_TIME_POINT.plus(Duration.ONE_MINUTE))), dataCases, clientSession)));
    }
}
