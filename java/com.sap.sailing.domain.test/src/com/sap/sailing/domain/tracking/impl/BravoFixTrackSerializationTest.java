package com.sap.sailing.domain.tracking.impl;

import static com.sap.sse.common.impl.MillisecondsTimePoint.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.sensordata.BravoSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.impl.BravoFixImpl;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.DynamicBravoFixTrack;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public class BravoFixTrackSerializationTest {
    
    private DynamicBravoFixTrack<Competitor> track;
    
    @BeforeEach
    public void setUp() {
        CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("SAP Extreme Sailing Team");
        track = new BravoFixTrackImpl<Competitor>(competitor, BravoFixTrack.TRACK_NAME, false);
    }
    
    @Test
    public void testWithZeroValueFix() throws ClassNotFoundException, IOException {
        addToTrackAndAssertRideHeight(new TestFixData(now().minus(Duration.ONE_MINUTE), 0.0));
    }
    
    @Test
    public void testWithSingleFix() throws ClassNotFoundException, IOException {
        addToTrackAndAssertRideHeight(new TestFixData(now().minus(Duration.ONE_MINUTE), 0.6543));
    }
    
    @Test
    public void testWithMultipleFixes() throws ClassNotFoundException, IOException {
        addToTrackAndAssertRideHeight(new TestFixData(now().minus(Duration.ONE_MINUTE), 1.0249),
                new TestFixData(now().minus(Duration.ONE_MINUTE.times(2)), 0.9140),
                new TestFixData(now().minus(Duration.ONE_MINUTE.times(3)), 0.3721),
                new TestFixData(now().minus(Duration.ONE_MINUTE.times(4)), 0.7942),
                new TestFixData(now().minus(Duration.ONE_MINUTE.times(5)), 0.6203));
    }
    
    @Test
    public void testWithTwoFixesWithSameTimePointNoReplacement() throws ClassNotFoundException, IOException {
        TimePoint timePoint = now().minus(Duration.ONE_MINUTE);
        TestFixData testFixData1 = new TestFixData(timePoint, 1.0000);
        TestFixData testFixData2 = new TestFixData(timePoint, 0.5000);
        Arrays.asList(testFixData1, testFixData2).forEach(TestFixData::addBravoFixToTrack);
        assertRideHeight(getDeserializedTrack(), testFixData1);
    }
    
    @Test
    public void testWithTwoFixesWithSameTimePointWithReplacement() throws ClassNotFoundException, IOException {
        TimePoint timePoint = now().minus(Duration.ONE_MINUTE);
        TestFixData testFixData1 = new TestFixData(timePoint, 1.0000);
        TestFixData testFixData2 = new TestFixData(timePoint, 0.5000);
        Arrays.asList(testFixData1, testFixData2).forEach(TestFixData::replaceBravoFixOnTrack);
        assertRideHeight(getDeserializedTrack(), testFixData2);
    }
    
    private void addToTrackAndAssertRideHeight(TestFixData... testFixData) throws ClassNotFoundException, IOException {
        List<TestFixData> testFixDataList = Arrays.asList(testFixData);
        testFixDataList.forEach(TestFixData::addBravoFixToTrack);
        BravoFixTrack<Competitor> track = getDeserializedTrack();
        testFixDataList.forEach(testData -> assertRideHeight(track, testData));
    }
    
    private void assertRideHeight(BravoFixTrack<Competitor> track, TestFixData testFixData) {
        assertRideHeight(track, testFixData.timePoint, testFixData.rideHeight);
    }
    
    private void assertRideHeight(BravoFixTrack<Competitor> track, TimePoint timePoint, double expectedRideHeightInMeters) {
        assertEquals(expectedRideHeightInMeters, track.getRideHeight(timePoint).getMeters(), 0.0);
    }
    
    @SuppressWarnings("unchecked")
    private BravoFixTrack<Competitor> getDeserializedTrack() throws ClassNotFoundException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(track);
        ous.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        BravoFixTrack<Competitor> deserializedInstance = (BravoFixTrack<Competitor>) ois.readObject();
        ois.close();
        return deserializedInstance;
    }
    
    private class TestFixData {
        private final TimePoint timePoint;
        private final double rideHeight;
        
        private TestFixData(TimePoint timePoint, double rideHeight) {
            this.timePoint = timePoint;
            this.rideHeight = rideHeight;
        }
        
        private void addBravoFixToTrack() {
            addOrReplaceBravoFixToTrack(false);
        }
        
        private void replaceBravoFixOnTrack() {
            addOrReplaceBravoFixToTrack(true);
        }
        
        private void addOrReplaceBravoFixToTrack(boolean replace) {
            Double[] fixData = new Double[BravoSensorDataMetadata.getTrackColumnCount()];
            // fill the port/starboard columns as well because their minimum defines the true ride height
            fixData[BravoSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex()] = rideHeight;
            fixData[BravoSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex()] = rideHeight;
            track.add(new BravoFixImpl(new DoubleVectorFixImpl(timePoint, fixData)), replace);
        }
    }

}
